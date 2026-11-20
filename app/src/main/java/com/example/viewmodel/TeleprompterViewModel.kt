package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRouteMonitor
import com.example.audio.TeleprompterTtsEngine
import com.example.camera.CameraBindResult
import com.example.camera.CameraRecorderController
import com.example.data.LocalScriptRepository
import com.example.domain.PortugueseSentenceSplitter
import com.example.model.AppScreen
import com.example.model.AudioRouteInfo
import com.example.model.CameraHardwareCapabilities
import com.example.model.CameraSettingsState
import com.example.model.PanelHeightMode
import com.example.model.PlaybackState
import com.example.model.RecordingState
import com.example.model.ScriptDocument
import com.example.model.TeleprompterPreferences
import com.example.model.TtsAvailability
import com.example.model.UpdateCheckState
import com.example.model.VoiceOption
import com.example.model.WhiteBalancePreset
import com.example.update.GitHubUpdateChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

data class SetupUiState(
    val currentScript: ScriptDocument = ScriptDocument(),
    val savedScripts: List<ScriptDocument> = emptyList(),
    val hasUnsavedChanges: Boolean = false,
    val statusBanner: String? = null,
)

data class StudioUiState(
    val isFrontCamera: Boolean = true,
    val hasOtherCamera: Boolean = true,
    val effectiveQualityLabel: String = "1080p",
    val cameraReady: Boolean = false,
    val cameraError: String? = null,
    val isScriptPanelExpanded: Boolean = true,
    val headsetDisconnectedWarning: String? = null,
    val interruptionMessage: String? = null,
)

class TeleprompterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalScriptRepository(application)

    private val _currentScreen = MutableStateFlow(AppScreen.SETUP)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _preferences = MutableStateFlow(repository.loadPreferences())
    val preferences: StateFlow<TeleprompterPreferences> = _preferences.asStateFlow()

    private val _setupState = MutableStateFlow(SetupUiState())
    val setupState: StateFlow<SetupUiState> = _setupState.asStateFlow()

    private val _studioState = MutableStateFlow(
        StudioUiState(isFrontCamera = _preferences.value.preferFrontCamera)
    )
    val studioState: StateFlow<StudioUiState> = _studioState.asStateFlow()

    private val _cameraCapabilities = MutableStateFlow(CameraHardwareCapabilities())
    val cameraCapabilities: StateFlow<CameraHardwareCapabilities> = _cameraCapabilities.asStateFlow()

    private val _cameraSettings = MutableStateFlow(CameraSettingsState())
    val cameraSettings: StateFlow<CameraSettingsState> = _cameraSettings.asStateFlow()

    private val _cameraRebindVersion = MutableStateFlow(0)
    val cameraRebindVersion: StateFlow<Int> = _cameraRebindVersion.asStateFlow()

    private val _githubRepoSlug = MutableStateFlow(repository.loadGithubRepo())
    val githubRepoSlug: StateFlow<String> = _githubRepoSlug.asStateFlow()

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    val ttsEngine: TeleprompterTtsEngine = TeleprompterTtsEngine(
        context = application,
        scope = viewModelScope,
        onAudioFocusInterrupted = {
            _studioState.update {
                it.copy(
                    interruptionMessage = "Áudio interrompido por outro app ou chamada. Leitura pausada."
                )
            }
        },
    )

    val audioRouteMonitor: AudioRouteMonitor = AudioRouteMonitor(
        context = application,
        onHeadsetDisconnected = { handleHeadsetDisconnected() },
    )

    val cameraController: CameraRecorderController = CameraRecorderController(application)

    val ttsAvailability: StateFlow<TtsAvailability> = ttsEngine.availability
    val voices: StateFlow<List<VoiceOption>> = ttsEngine.voices
    val selectedVoiceName: StateFlow<String?> = ttsEngine.selectedVoiceName
    val isPlayingSample: StateFlow<Boolean> = ttsEngine.isPlayingSample
    val playbackState: StateFlow<PlaybackState> = ttsEngine.playbackState
    val audioRoute: StateFlow<AudioRouteInfo> = audioRouteMonitor.routeInfo

    private var countdownJob: Job? = null
    private var recordingElapsedBackupMillis: Long = 0L

    init {
        audioRouteMonitor.start()
        val prefs = _preferences.value
        ttsEngine.updateSettings(
            voiceName = prefs.selectedVoiceName,
            rate = prefs.speechRate,
            pauseSec = prefs.pauseSeconds,
        )
        loadPersistedScripts()
    }

    private fun loadPersistedScripts() {
        val scripts = repository.loadScripts()
        val lastId = repository.loadLastScriptId()
        val active = scripts.firstOrNull { it.id == lastId }
            ?: scripts.firstOrNull()
            ?: ScriptDocument()

        _setupState.value = SetupUiState(
            currentScript = active,
            savedScripts = scripts,
            hasUnsavedChanges = false,
        )
        ttsEngine.setSentences(active.sentences, preserveIndex = false)
    }

    // --- Script Editing & Persistence ---

    fun onTitleChanged(newTitle: String) {
        if (_recordingState.value.isSessionLocked) return
        _setupState.update { state ->
            state.copy(
                currentScript = state.currentScript.copy(title = newTitle),
                hasUnsavedChanges = true,
                statusBanner = null,
            )
        }
    }

    fun onRawTextChanged(newText: String) {
        if (_recordingState.value.isSessionLocked) return
        _setupState.update { state ->
            val current = state.currentScript
            val updatedSentences = if (!current.hasManualSplitEdits) {
                PortugueseSentenceSplitter.split(newText)
            } else {
                current.sentences
            }
            val updatedDoc = current.copy(
                rawText = newText,
                sentences = updatedSentences,
            )
            ttsEngine.setSentences(updatedSentences, preserveIndex = false)
            state.copy(
                currentScript = updatedDoc,
                hasUnsavedChanges = true,
                statusBanner = null,
            )
        }
    }

    fun resplitFromRawText() {
        if (_recordingState.value.isSessionLocked) return
        _setupState.update { state ->
            val fresh = PortugueseSentenceSplitter.split(state.currentScript.rawText)
            ttsEngine.setSentences(fresh, preserveIndex = false)
            state.copy(
                currentScript = state.currentScript.copy(
                    sentences = fresh,
                    hasManualSplitEdits = false,
                ),
                hasUnsavedChanges = true,
                statusBanner = "Frases atualizadas a partir do texto (${fresh.size} frases).",
            )
        }
    }

    fun updateSentence(index: Int, newText: String) {
        if (_recordingState.value.isSessionLocked) return
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        _setupState.update { state ->
            val list = state.currentScript.sentences.toMutableList()
            if (index !in list.indices) return@update state
            list[index] = trimmed
            ttsEngine.setSentences(list, preserveIndex = true)
            state.copy(
                currentScript = state.currentScript.copy(
                    sentences = list,
                    hasManualSplitEdits = true,
                ),
                hasUnsavedChanges = true,
                statusBanner = null,
            )
        }
    }

    fun mergeSentenceWithNext(index: Int) {
        if (_recordingState.value.isSessionLocked) return
        _setupState.update { state ->
            val merged = PortugueseSentenceSplitter.mergeWithNext(state.currentScript.sentences, index)
            ttsEngine.setSentences(merged, preserveIndex = true)
            state.copy(
                currentScript = state.currentScript.copy(
                    sentences = merged,
                    hasManualSplitEdits = true,
                ),
                hasUnsavedChanges = true,
                statusBanner = "Frases ${index + 1} e ${index + 2} unidas.",
            )
        }
    }

    fun splitSentenceAtWord(index: Int, wordSplitIndex: Int) {
        if (_recordingState.value.isSessionLocked) return
        _setupState.update { state ->
            val splitList = PortugueseSentenceSplitter.splitSentenceAtWord(
                state.currentScript.sentences,
                index,
                wordSplitIndex,
            )
            ttsEngine.setSentences(splitList, preserveIndex = true)
            state.copy(
                currentScript = state.currentScript.copy(
                    sentences = splitList,
                    hasManualSplitEdits = true,
                ),
                hasUnsavedChanges = true,
                statusBanner = "Frase ${index + 1} dividida em duas partes.",
            )
        }
    }

    fun removeSentence(index: Int) {
        if (_recordingState.value.isSessionLocked) return
        _setupState.update { state ->
            val list = state.currentScript.sentences.toMutableList()
            if (index !in list.indices) return@update state
            list.removeAt(index)
            ttsEngine.setSentences(list, preserveIndex = true)
            state.copy(
                currentScript = state.currentScript.copy(
                    sentences = list,
                    hasManualSplitEdits = true,
                ),
                hasUnsavedChanges = true,
            )
        }
    }

    fun addSentenceAfter(index: Int, text: String) {
        if (_recordingState.value.isSessionLocked) return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _setupState.update { state ->
            val list = state.currentScript.sentences.toMutableList()
            val insertAt = (index + 1).coerceIn(0, list.size)
            list.add(insertAt, trimmed)
            ttsEngine.setSentences(list, preserveIndex = true)
            state.copy(
                currentScript = state.currentScript.copy(
                    sentences = list,
                    hasManualSplitEdits = true,
                ),
                hasUnsavedChanges = true,
            )
        }
    }

    fun saveCurrentScript(showFeedback: Boolean = true): Boolean {
        val current = _setupState.value.currentScript
        val sentences = current.sentences.ifEmpty {
            PortugueseSentenceSplitter.split(current.rawText)
        }
        val cleanTitle = current.title.trim().ifEmpty {
            sentences.firstOrNull()?.take(36)?.trimEnd('.', ',', ';') ?: "Roteiro sem título"
        }
        val finalized = current.copy(
            title = cleanTitle,
            sentences = sentences,
            updatedAtMillis = System.currentTimeMillis(),
        )
        val existingList = _setupState.value.savedScripts.toMutableList()
        val idx = existingList.indexOfFirst { it.id == finalized.id }
        if (idx >= 0) {
            existingList[idx] = finalized
        } else {
            existingList.add(0, finalized)
        }
        val sorted = existingList.sortedByDescending { it.updatedAtMillis }
        val ok = repository.saveScripts(sorted)
        if (ok) {
            repository.saveLastScriptId(finalized.id)
            ttsEngine.setSentences(finalized.sentences, preserveIndex = true)
            _setupState.update {
                it.copy(
                    currentScript = finalized,
                    savedScripts = sorted,
                    hasUnsavedChanges = false,
                    statusBanner = if (showFeedback) "Roteiro salvo localmente." else it.statusBanner,
                )
            }
        } else {
            _setupState.update {
                it.copy(statusBanner = "Não foi possível salvar o roteiro no armazenamento local.")
            }
        }
        return ok
    }

    fun openScript(script: ScriptDocument) {
        ttsEngine.stopSample()
        ttsEngine.stopAllAndResetToIdle(keepSelectedIndex = false)
        repository.saveLastScriptId(script.id)
        ttsEngine.setSentences(script.sentences, preserveIndex = false)
        _setupState.update {
            it.copy(
                currentScript = script,
                hasUnsavedChanges = false,
                statusBanner = "Roteiro \"${script.title}\" aberto.",
            )
        }
    }

    fun createNewScript() {
        ttsEngine.stopSample()
        ttsEngine.stopAllAndResetToIdle(keepSelectedIndex = false)
        val blank = ScriptDocument(id = UUID.randomUUID().toString())
        ttsEngine.setSentences(emptyList(), preserveIndex = false)
        _setupState.update {
            it.copy(
                currentScript = blank,
                hasUnsavedChanges = false,
                statusBanner = null,
            )
        }
    }

    fun deleteScript(scriptId: String) {
        val updated = _setupState.value.savedScripts.filterNot { it.id == scriptId }
        repository.saveScripts(updated)
        val currentWasDeleted = _setupState.value.currentScript.id == scriptId
        val nextCurrent = if (currentWasDeleted) {
            updated.firstOrNull() ?: ScriptDocument()
        } else {
            _setupState.value.currentScript
        }
        repository.saveLastScriptId(if (updated.any { it.id == nextCurrent.id }) nextCurrent.id else null)
        ttsEngine.setSentences(nextCurrent.sentences, preserveIndex = false)
        _setupState.update {
            it.copy(
                currentScript = nextCurrent,
                savedScripts = updated,
                hasUnsavedChanges = false,
                statusBanner = "Roteiro excluído.",
            )
        }
    }

    fun clearSetupBanner() {
        _setupState.update { it.copy(statusBanner = null) }
    }

    // --- Voice & Preferences ---

    fun selectVoice(voiceName: String) {
        if (_recordingState.value.isSessionLocked) return
        val updated = _preferences.value.copy(selectedVoiceName = voiceName)
        _preferences.value = updated
        repository.savePreferences(updated)
        ttsEngine.updateSettings(voiceName, updated.speechRate, updated.pauseSeconds)
    }

    fun updateSpeechRate(rate: Float) {
        if (_recordingState.value.isSessionLocked) return
        // Snap cleanly to 0.05 increments between 0.6 and 1.2
        val snapped = ((rate * 20f).roundToInt() / 20f).coerceIn(0.6f, 1.2f)
        val updated = _preferences.value.copy(speechRate = snapped)
        _preferences.value = updated
        repository.savePreferences(updated)
        ttsEngine.updateSettings(updated.selectedVoiceName, snapped, updated.pauseSeconds)
    }

    fun updatePauseSeconds(seconds: Float) {
        if (_recordingState.value.isSessionLocked) return
        // Exact 0.5 second steps from 0.0 to 5.0
        val snapped = ((seconds * 2f).roundToInt() / 2f).coerceIn(0f, 5.0f)
        val updated = _preferences.value.copy(pauseSeconds = snapped)
        _preferences.value = updated
        repository.savePreferences(updated)
        ttsEngine.updateSettings(updated.selectedVoiceName, updated.speechRate, snapped)
    }

    fun toggleVoiceSample() {
        ttsEngine.toggleSample(
            "Olá! Esta é a voz guia do Escuta e Grava. " +
                "Confira se o som está claro no seu fone antes de iniciar a gravação."
        )
    }

    fun refreshTtsAndRoute() {
        audioRouteMonitor.refreshRoute(triggerDisconnectIfLost = false)
        ttsEngine.refreshVoices()
    }

    // --- Navigation between Setup and Studio ---

    fun openRecordingStudio() {
        val current = _setupState.value.currentScript
        val sentences = current.sentences.ifEmpty {
            PortugueseSentenceSplitter.split(current.rawText)
        }
        ttsEngine.stopSample()
        if (sentences.isNotEmpty()) {
            saveCurrentScript(showFeedback = false)
        }
        ttsEngine.setSentences(sentences, preserveIndex = false)
        ttsEngine.stopAllAndResetToIdle(keepSelectedIndex = false)
        audioRouteMonitor.refreshRoute(triggerDisconnectIfLost = false)
        _recordingState.value = RecordingState.Idle
        _studioState.update {
            it.copy(
                headsetDisconnectedWarning = null,
                interruptionMessage = null,
                cameraError = null,
            )
        }
        _currentScreen.value = AppScreen.STUDIO
    }

    fun exitStudioToSetup() {
        countdownJob?.cancel()
        countdownJob = null
        ttsEngine.stopAllAndResetToIdle(keepSelectedIndex = true)
        if (_recordingState.value.isSessionLocked) {
            cameraController.stopRecording()
        }
        cameraController.release()
        _recordingState.value = RecordingState.Idle
        _studioState.update {
            it.copy(
                cameraReady = false,
                headsetDisconnectedWarning = null,
                interruptionMessage = null,
            )
        }
        _currentScreen.value = AppScreen.SETUP
    }

    // --- GitHub Update Checker ---

    fun updateGithubRepoSlug(slug: String) {
        _githubRepoSlug.value = slug
        repository.saveGithubRepo(slug)
    }

    fun checkForGithubUpdate() {
        val slug = _githubRepoSlug.value.trim()
        repository.saveGithubRepo(slug)
        _updateCheckState.value = UpdateCheckState.Checking
        viewModelScope.launch {
            _updateCheckState.value = GitHubUpdateChecker.checkLatestRelease(slug)
        }
    }

    fun resetUpdateCheckState() {
        _updateCheckState.value = UpdateCheckState.Idle
    }

    // --- Studio Overlay & Pro Camera Controls ---

    fun onCameraBound(result: CameraBindResult) {
        _cameraCapabilities.value = result.capabilities
        _cameraSettings.update { prev ->
            val clampedZoom = prev.zoomRatio.coerceIn(
                result.capabilities.minZoomRatio,
                result.capabilities.maxZoomRatio.coerceAtLeast(result.capabilities.minZoomRatio),
            )
            val clampedEv = if (result.capabilities.supportsExposureCompensation) {
                prev.exposureIndex.coerceIn(
                    result.capabilities.exposureMinIndex,
                    result.capabilities.exposureMaxIndex,
                )
            } else {
                0
            }
            val validAperture = prev.selectedAperture?.takeIf { it in result.capabilities.availableApertures }
                ?: result.capabilities.availableApertures.firstOrNull()
            prev.copy(
                selectedCameraId = result.boundCameraId ?: prev.selectedCameraId,
                zoomRatio = clampedZoom,
                exposureIndex = clampedEv,
                selectedAperture = validAperture,
                manualExposureEnabled = prev.manualExposureEnabled && result.capabilities.supportsManualExposure,
                manualIso = prev.manualIso.coerceIn(result.capabilities.isoMin, result.capabilities.isoMax),
                manualShutterNs = prev.manualShutterNs.coerceIn(
                    result.capabilities.shutterMinNs,
                    result.capabilities.shutterMaxNs,
                ),
                torchEnabled = prev.torchEnabled && result.capabilities.hasFlashUnit,
            )
        }
        _studioState.update {
            it.copy(
                isFrontCamera = result.isFrontCamera,
                hasOtherCamera = result.hasOtherCamera,
                effectiveQualityLabel = result.effectiveQualityLabel,
                cameraReady = result.videoCaptureSupported,
                cameraError = result.warningMessage,
            )
        }
    }

    fun onCameraError(message: String) {
        _studioState.update {
            it.copy(
                cameraReady = false,
                cameraError = message,
            )
        }
    }

    fun toggleCameraLens() {
        if (_recordingState.value.isSessionLocked) return
        val nextFront = !_studioState.value.isFrontCamera
        val updatedPrefs = _preferences.value.copy(preferFrontCamera = nextFront)
        _preferences.value = updatedPrefs
        repository.savePreferences(updatedPrefs)
        _cameraSettings.update {
            it.copy(
                selectedCameraId = null,
                zoomRatio = 1.0f,
                exposureIndex = 0,
                manualExposureEnabled = false,
                aeLocked = false,
                torchEnabled = false,
            )
        }
        _studioState.update { it.copy(isFrontCamera = nextFront, cameraReady = false) }
        _cameraRebindVersion.update { it + 1 }
    }

    fun selectCameraLensById(cameraId: String, isFront: Boolean) {
        if (_recordingState.value.isSessionLocked) return
        val updatedPrefs = _preferences.value.copy(preferFrontCamera = isFront)
        _preferences.value = updatedPrefs
        repository.savePreferences(updatedPrefs)
        _cameraSettings.update {
            it.copy(
                selectedCameraId = cameraId,
                zoomRatio = 1.0f,
                exposureIndex = 0,
                manualExposureEnabled = false,
                aeLocked = false,
                torchEnabled = false,
            )
        }
        _studioState.update { it.copy(isFrontCamera = isFront, cameraReady = false) }
        _cameraRebindVersion.update { it + 1 }
    }

    fun selectVideoQuality(qualityLabel: String) {
        if (_recordingState.value.isSessionLocked) return
        _cameraSettings.update { it.copy(selectedQualityLabel = qualityLabel) }
        _studioState.update { it.copy(cameraReady = false) }
        _cameraRebindVersion.update { it + 1 }
    }

    fun selectVideoFps(fps: Int) {
        if (_recordingState.value.isSessionLocked) return
        _cameraSettings.update { it.copy(selectedFps = fps) }
        _studioState.update { it.copy(cameraReady = false) }
        _cameraRebindVersion.update { it + 1 }
    }

    fun setVideoStabilization(enabled: Boolean) {
        if (_recordingState.value.isSessionLocked) return
        _cameraSettings.update { it.copy(videoStabilizationEnabled = enabled) }
        _studioState.update { it.copy(cameraReady = false) }
        _cameraRebindVersion.update { it + 1 }
    }

    fun setCameraZoom(ratio: Float) {
        val caps = _cameraCapabilities.value
        val clamped = ratio.coerceIn(caps.minZoomRatio, caps.maxZoomRatio.coerceAtLeast(caps.minZoomRatio))
        updateLiveCameraSettings { it.copy(zoomRatio = clamped) }
    }

    fun setCameraExposureIndex(index: Int) {
        val caps = _cameraCapabilities.value
        val clamped = index.coerceIn(caps.exposureMinIndex, caps.exposureMaxIndex)
        updateLiveCameraSettings { it.copy(exposureIndex = clamped, manualExposureEnabled = false) }
    }

    fun setCameraAperture(aperture: Float) {
        updateLiveCameraSettings { it.copy(selectedAperture = aperture) }
    }

    fun setManualExposureEnabled(enabled: Boolean) {
        if (!_cameraCapabilities.value.supportsManualExposure) return
        updateLiveCameraSettings { it.copy(manualExposureEnabled = enabled, aeLocked = false) }
    }

    fun setManualIso(iso: Int) {
        val caps = _cameraCapabilities.value
        val clamped = iso.coerceIn(caps.isoMin, caps.isoMax)
        updateLiveCameraSettings { it.copy(manualExposureEnabled = true, manualIso = clamped) }
    }

    fun setManualShutterNs(shutterNs: Long) {
        val caps = _cameraCapabilities.value
        val clamped = shutterNs.coerceIn(caps.shutterMinNs, caps.shutterMaxNs)
        updateLiveCameraSettings { it.copy(manualExposureEnabled = true, manualShutterNs = clamped) }
    }

    fun setWhiteBalance(preset: WhiteBalancePreset) {
        updateLiveCameraSettings { it.copy(whiteBalance = preset) }
    }

    fun setTorchEnabled(enabled: Boolean) {
        if (!_cameraCapabilities.value.hasFlashUnit) return
        updateLiveCameraSettings { it.copy(torchEnabled = enabled) }
    }

    fun setAeLocked(locked: Boolean) {
        updateLiveCameraSettings { it.copy(aeLocked = locked, manualExposureEnabled = false) }
    }

    fun resetCameraControlsToAuto() {
        val caps = _cameraCapabilities.value
        updateLiveCameraSettings {
            it.copy(
                zoomRatio = 1.0f.coerceIn(caps.minZoomRatio, caps.maxZoomRatio.coerceAtLeast(1.0f)),
                exposureIndex = 0,
                manualExposureEnabled = false,
                selectedAperture = caps.availableApertures.firstOrNull(),
                whiteBalance = WhiteBalancePreset.AUTO,
                aeLocked = false,
                torchEnabled = false,
            )
        }
    }

    private fun updateLiveCameraSettings(transform: (CameraSettingsState) -> CameraSettingsState) {
        val next = transform(_cameraSettings.value)
        _cameraSettings.value = next
        cameraController.applyLiveControls(next, _cameraCapabilities.value)
    }

    fun toggleScriptPanelExpanded() {
        _studioState.update { it.copy(isScriptPanelExpanded = !it.isScriptPanelExpanded) }
    }

    fun cyclePanelHeight() {
        val nextMode = _preferences.value.panelHeightMode.next()
        val updated = _preferences.value.copy(panelHeightMode = nextMode)
        _preferences.value = updated
        repository.savePreferences(updated)
    }

    fun adjustOverlayFontSize(deltaSp: Float) {
        val nextSize = (_preferences.value.overlayFontSizeSp + deltaSp).coerceIn(14f, 28f)
        val updated = _preferences.value.copy(overlayFontSizeSp = nextSize)
        _preferences.value = updated
        repository.savePreferences(updated)
    }

    // --- Independent Reading Controls (Never stop/pause/restart Recording!) ---

    fun onStudioPlayPauseReading() {
        val rec = _recordingState.value
        val isRehearsal = rec !is RecordingState.Recording && rec !is RecordingState.Countdown
        clearHeadsetWarningIfSafe()
        ttsEngine.togglePlayPause(isRehearsal = isRehearsal)
    }

    fun onStudioPhraseTapped(index: Int) {
        val rec = _recordingState.value
        val isRehearsal = rec !is RecordingState.Recording && rec !is RecordingState.Countdown
        clearHeadsetWarningIfSafe()
        ttsEngine.onPhraseTapped(index = index, isRehearsal = isRehearsal)
    }

    fun onStudioRepeatPhrase() {
        val rec = _recordingState.value
        val isRehearsal = rec !is RecordingState.Recording && rec !is RecordingState.Countdown
        clearHeadsetWarningIfSafe()
        ttsEngine.repeatCurrentPhrase(isRehearsal = isRehearsal)
    }

    fun onStudioPreviousPhrase() {
        val rec = _recordingState.value
        val isRehearsal = rec !is RecordingState.Recording && rec !is RecordingState.Countdown
        clearHeadsetWarningIfSafe()
        ttsEngine.previousPhrase(isRehearsal = isRehearsal)
    }

    fun onStudioNextPhrase() {
        val rec = _recordingState.value
        val isRehearsal = rec !is RecordingState.Recording && rec !is RecordingState.Countdown
        clearHeadsetWarningIfSafe()
        ttsEngine.nextPhrase(isRehearsal = isRehearsal)
    }

    private fun clearHeadsetWarningIfSafe() {
        if (audioRoute.value.isHeadsetSafe) {
            _studioState.update { it.copy(headsetDisconnectedWarning = null) }
        }
    }

    fun dismissStudioAlerts() {
        _studioState.update {
            it.copy(
                headsetDisconnectedWarning = null,
                interruptionMessage = null,
            )
        }
        ttsEngine.clearError()
        ttsEngine.clearStatusNote()
    }

    // --- Recording Lifecycle ---

    fun startRecordingSession(isShortTest: Boolean = false) {
        if (_recordingState.value.isSessionLocked) return
        if (!_studioState.value.cameraReady) {
            _recordingState.value = RecordingState.Error("Aguarde a câmera ficar pronta antes de gravar.")
            return
        }

        // Cancel any active rehearsal before starting recording; preserve selected phrase index
        countdownJob?.cancel()
        ttsEngine.stopAllAndResetToIdle(keepSelectedIndex = true)
        recordingElapsedBackupMillis = 0L
        _studioState.update {
            it.copy(
                headsetDisconnectedWarning = null,
                interruptionMessage = null,
            )
        }
        _recordingState.value = RecordingState.Starting(isShortTest = isShortTest)

        val launched = cameraController.startRecording(
            isShortTest = isShortTest,
            onStarted = {
                // Exact requirement: 3-second visual countdown starts ONLY AFTER CameraX confirms recording started
                beginPostStartCountdown(isShortTest = isShortTest)
            },
            onStatus = { elapsedMs, amplitude, hasAudio ->
                recordingElapsedBackupMillis = elapsedMs
                val current = _recordingState.value
                if (current is RecordingState.Recording) {
                    _recordingState.value = current.copy(
                        elapsedMillis = elapsedMs,
                        audioAmplitude = amplitude,
                        hasAudioTrack = hasAudio,
                    )
                }
            },
            onFinalizedSuccess = { uri: Uri, displayName: String, durationMillis: Long, qualityLabel: String ->
                countdownJob?.cancel()
                ttsEngine.stopAllAndResetToIdle(keepSelectedIndex = true)
                _recordingState.value = RecordingState.Saved(
                    uriString = uri.toString(),
                    displayName = displayName,
                    durationMillis = durationMillis.coerceAtLeast(recordingElapsedBackupMillis),
                    qualityLabel = qualityLabel,
                    isShortTest = isShortTest,
                )
            },
            onFinalizedError = { errMessage ->
                countdownJob?.cancel()
                ttsEngine.stopAllAndResetToIdle(keepSelectedIndex = true)
                _recordingState.value = RecordingState.Error(errMessage)
            },
        )

        if (!launched) {
            if (_recordingState.value is RecordingState.Starting) {
                _recordingState.value = RecordingState.Error("Não foi possível iniciar a gravação.")
            }
        }
    }

    private fun beginPostStartCountdown(isShortTest: Boolean) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (sec in 3 downTo 1) {
                if (_recordingState.value !is RecordingState.Starting &&
                    _recordingState.value !is RecordingState.Countdown
                ) {
                    return@launch
                }
                _recordingState.value = RecordingState.Countdown(
                    secondsRemaining = sec,
                    isShortTest = isShortTest,
                )
                delay(1000L)
            }

            if (_recordingState.value is RecordingState.Countdown) {
                _recordingState.value = RecordingState.Recording(
                    elapsedMillis = recordingElapsedBackupMillis,
                    isShortTest = isShortTest,
                )
                // Start reading automatically from selected phrase once countdown completes
                val selectedIdx = ttsEngine.playbackState.value.currentIndex
                ttsEngine.startContinuous(fromIndex = selectedIdx, isRehearsal = false)
            }
        }
    }

    fun stopRecordingSession() {
        val current = _recordingState.value
        val isShortTest = when (current) {
            is RecordingState.Starting -> current.isShortTest
            is RecordingState.Countdown -> current.isShortTest
            is RecordingState.Recording -> current.isShortTest
            else -> return
        }
        countdownJob?.cancel()
        countdownJob = null
        // Cancel reading & pending tasks immediately, then wait for CameraX Finalize
        ttsEngine.stopAllAndResetToIdle(keepSelectedIndex = true)
        _recordingState.value = RecordingState.Finalizing(isShortTest = isShortTest)
        cameraController.stopRecording()
    }

    fun dismissRecordingResultOrError() {
        if (_recordingState.value is RecordingState.Saved || _recordingState.value is RecordingState.Error) {
            _recordingState.value = RecordingState.Idle
        }
    }

    /**
     * Called if Activity goes to background unexpectedly while on Studio screen.
     */
    fun onHostPaused() {
        if (_currentScreen.value == AppScreen.STUDIO) {
            ttsEngine.pauseReading(note = "Sessão interrompida ao sair do aplicativo.")
            if (_recordingState.value is RecordingState.Recording ||
                _recordingState.value is RecordingState.Countdown ||
                _recordingState.value is RecordingState.Starting
            ) {
                stopRecordingSession()
            }
        } else {
            ttsEngine.stopSample()
        }
    }

    private fun handleHeadsetDisconnected() {
        val playing = ttsEngine.playbackState.value.isActiveContinuous ||
            ttsEngine.playbackState.value.mode == com.example.model.PlaybackMode.SPEAKING_SINGLE ||
            ttsEngine.isPlayingSample.value

        if (playing) {
            ttsEngine.pauseReading(
                note = "Fone desconectado — leitura pausada para não sair no alto-falante."
            )
            _studioState.update {
                val recActive = _recordingState.value is RecordingState.Recording ||
                    _recordingState.value is RecordingState.Countdown
                val msg = if (recActive) {
                    "Fone desconectado — leitura pausada. A gravação de vídeo continua."
                } else {
                    "Fone desconectado — leitura pausada para evitar som no alto-falante."
                }
                it.copy(headsetDisconnectedWarning = msg)
            }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        audioRouteMonitor.stop()
        ttsEngine.release()
        cameraController.release()
        super.onCleared()
    }
}
