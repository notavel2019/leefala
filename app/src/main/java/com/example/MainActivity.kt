package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppScreen
import com.example.ui.setup.ScriptSetupScreen
import com.example.ui.studio.RecordingStudioScreen
import com.example.ui.theme.EscutaEGravaTheme
import com.example.viewmodel.TeleprompterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TeleprompterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EscutaEGravaTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val setupState by viewModel.setupState.collectAsStateWithLifecycle()
                val studioState by viewModel.studioState.collectAsStateWithLifecycle()
                val preferences by viewModel.preferences.collectAsStateWithLifecycle()
                val audioRoute by viewModel.audioRoute.collectAsStateWithLifecycle()
                val ttsAvailability by viewModel.ttsAvailability.collectAsStateWithLifecycle()
                val voices by viewModel.voices.collectAsStateWithLifecycle()
                val selectedVoiceName by viewModel.selectedVoiceName.collectAsStateWithLifecycle()
                val isPlayingSample by viewModel.isPlayingSample.collectAsStateWithLifecycle()
                val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
                val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
                val cameraCapabilities by viewModel.cameraCapabilities.collectAsStateWithLifecycle()
                val cameraSettings by viewModel.cameraSettings.collectAsStateWithLifecycle()
                val cameraRebindVersion by viewModel.cameraRebindVersion.collectAsStateWithLifecycle()
                val githubRepoSlug by viewModel.githubRepoSlug.collectAsStateWithLifecycle()
                val updateCheckState by viewModel.updateCheckState.collectAsStateWithLifecycle()

                when (currentScreen) {
                    AppScreen.SETUP -> {
                        ScriptSetupScreen(
                            setupState = setupState,
                            preferences = preferences,
                            audioRoute = audioRoute,
                            ttsAvailability = ttsAvailability,
                            voices = voices,
                            selectedVoiceName = selectedVoiceName,
                            isPlayingSample = isPlayingSample,
                            onTitleChanged = viewModel::onTitleChanged,
                            onRawTextChanged = viewModel::onRawTextChanged,
                            onResplitFromRawText = viewModel::resplitFromRawText,
                            onUpdateSentence = viewModel::updateSentence,
                            onMergeWithNext = viewModel::mergeSentenceWithNext,
                            onSplitAtWord = viewModel::splitSentenceAtWord,
                            onRemoveSentence = viewModel::removeSentence,
                            onAddSentenceAfter = viewModel::addSentenceAfter,
                            onSaveScript = { viewModel.saveCurrentScript(showFeedback = true) },
                            onOpenScript = viewModel::openScript,
                            onCreateNewScript = viewModel::createNewScript,
                            onDeleteScript = viewModel::deleteScript,
                            onClearBanner = viewModel::clearSetupBanner,
                            onSelectVoice = viewModel::selectVoice,
                            onSpeechRateChange = viewModel::updateSpeechRate,
                            onPauseSecondsChange = viewModel::updatePauseSeconds,
                            onToggleSample = viewModel::toggleVoiceSample,
                            onRefreshTtsAndRoute = viewModel::refreshTtsAndRoute,
                            onOpenBluetoothSettings = ::openBluetoothSettings,
                            onInstallTtsData = ::launchTtsInstall,
                            onOpenTtsSettings = ::openTtsSettings,
                            onOpenStudio = viewModel::openRecordingStudio,
                            githubRepoSlug = githubRepoSlug,
                            updateCheckState = updateCheckState,
                            onGithubRepoSlugChanged = viewModel::updateGithubRepoSlug,
                            onCheckGithubUpdate = viewModel::checkForGithubUpdate,
                            onResetUpdateState = viewModel::resetUpdateCheckState,
                            onOpenExternalUrl = ::openExternalUrl,
                        )
                    }

                    AppScreen.STUDIO -> {
                        RecordingStudioScreen(
                            scriptTitle = setupState.currentScript.title,
                            sentences = setupState.currentScript.sentences,
                            preferences = preferences,
                            studioState = studioState,
                            playbackState = playbackState,
                            recordingState = recordingState,
                            audioRoute = audioRoute,
                            cameraCapabilities = cameraCapabilities,
                            cameraSettings = cameraSettings,
                            cameraRebindVersion = cameraRebindVersion,
                            cameraController = viewModel.cameraController,
                            onCameraBound = viewModel::onCameraBound,
                            onCameraError = viewModel::onCameraError,
                            onToggleCameraLens = viewModel::toggleCameraLens,
                            onSelectCameraLens = viewModel::selectCameraLensById,
                            onSelectAperture = viewModel::setCameraAperture,
                            onZoomChange = viewModel::setCameraZoom,
                            onExposureIndexChange = viewModel::setCameraExposureIndex,
                            onManualExposureToggle = viewModel::setManualExposureEnabled,
                            onManualIsoChange = viewModel::setManualIso,
                            onManualShutterChange = viewModel::setManualShutterNs,
                            onSelectWhiteBalance = viewModel::setWhiteBalance,
                            onSelectQuality = viewModel::selectVideoQuality,
                            onSelectFps = viewModel::selectVideoFps,
                            onVideoStabilizationToggle = viewModel::setVideoStabilization,
                            onTorchToggle = viewModel::setTorchEnabled,
                            onAeLockToggle = viewModel::setAeLocked,
                            onResetCameraControls = viewModel::resetCameraControlsToAuto,
                            onTogglePanelExpanded = viewModel::toggleScriptPanelExpanded,
                            onCyclePanelHeight = viewModel::cyclePanelHeight,
                            onAdjustFontSize = viewModel::adjustOverlayFontSize,
                            onPhraseTapped = viewModel::onStudioPhraseTapped,
                            onPlayPauseReading = viewModel::onStudioPlayPauseReading,
                            onRepeatPhrase = viewModel::onStudioRepeatPhrase,
                            onPreviousPhrase = viewModel::onStudioPreviousPhrase,
                            onNextPhrase = viewModel::onStudioNextPhrase,
                            onStartRecording = viewModel::startRecordingSession,
                            onStopRecording = viewModel::stopRecordingSession,
                            onDismissAlerts = viewModel::dismissStudioAlerts,
                            onDismissRecordingResult = viewModel::dismissRecordingResultOrError,
                            onOpenVideo = ::openSavedVideo,
                            onShareVideo = ::shareSavedVideo,
                            onOpenAppSettings = ::openAppSettings,
                            onExitStudio = viewModel::exitStudioToSetup,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTtsAndRoute()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onHostPaused()
    }

    private fun openBluetoothSettings() {
        safeStartActivity(
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
            fallbackMessage = "Abra as configurações de Bluetooth do aparelho manualmente.",
        )
    }

    private fun launchTtsInstall() {
        safeStartActivity(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            fallbackMessage = "Não foi possível abrir o instalador de voz. Abra Configurações > Acessibilidade > Conversão de texto em voz.",
        )
    }

    private fun openTtsSettings() {
        val intent = Intent("com.android.settings.TTS_SETTINGS")
        safeStartActivity(
            intent,
            fallbackMessage = "Abra as configurações de Conversão de texto em voz nas Configurações do Android.",
        )
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        safeStartActivity(
            intent,
            fallbackMessage = "Abra as configurações do aplicativo para conceder câmera e microfone.",
        )
    }

    private fun openExternalUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        safeStartActivity(
            intent,
            fallbackMessage = "Não foi possível abrir o navegador.",
        )
    }

    private fun openSavedVideo(uriString: String) {
        val uri = Uri.parse(uriString)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        safeStartActivity(
            Intent.createChooser(intent, "Abrir vídeo com"),
            fallbackMessage = "Nenhum player de vídeo encontrado. O arquivo está na galeria em Movies/EscutaEGrava.",
        )
    }

    private fun shareSavedVideo(uriString: String) {
        val uri = Uri.parse(uriString)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        safeStartActivity(
            Intent.createChooser(intent, "Compartilhar vídeo"),
            fallbackMessage = "Não foi possível abrir o compartilhamento. O vídeo está salvo na galeria.",
        )
    }

    private fun safeStartActivity(intent: Intent, fallbackMessage: String) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, fallbackMessage, Toast.LENGTH_LONG).show()
        }
    }
}
