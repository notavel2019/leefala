package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlin.math.roundToInt
import com.example.model.PlaybackMode
import com.example.model.PlaybackState
import com.example.model.TtsAvailability
import com.example.model.VoiceOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Deterministic Android TextToSpeech controller for the auditory teleprompter.
 *
 * Design guarantees:
 * - Uses USAGE_MEDIA + CONTENT_TYPE_SPEECH (never forces Bluetooth SCO call mode).
 * - Each utterance receives a strictly unique token; stale callbacks from prior taps are ignored.
 * - Next phrase is scheduled ONLY after TTS onDone + configured pause delay.
 * - Rapid taps flush the TTS queue immediately and honor only the latest selection.
 * - Handles AudioFocus loss by pausing cleanly without touching video recording.
 */
class TeleprompterTtsEngine(
    context: Context,
    private val scope: CoroutineScope,
    private val onAudioFocusInterrupted: () -> Unit = {},
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var tts: TextToSpeech? = null
    private var voiceMap: Map<String, Voice> = emptyMap()
    private var currentSentences: List<String> = emptyList()
    private var speechRate: Float = 1.0f
    private var speechPitch: Float = 0.94f
    private var pauseSeconds: Float = 1.0f
    private var naturalProsodyEnabled: Boolean = true
    private var preferGoogleTtsEngine: Boolean = true
    private var currentlyUsingGoogleEngine: Boolean = false
    private var fallbackToDefaultAttempted: Boolean = false
    private var preferredVoiceName: String? = null
    private var returnVolume: Float = 0.75f
    private var effectiveTtsGain: Float = 1.0f

    private val generationCounter = AtomicLong(0L)
    @Volatile
    private var activeUtteranceId: String? = null
    private var pauseDelayJob: Job? = null

    private val _availability = MutableStateFlow(TtsAvailability.INITIALIZING)
    val availability: StateFlow<TtsAvailability> = _availability.asStateFlow()

    private val _voices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val voices: StateFlow<List<VoiceOption>> = _voices.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow<String?>(null)
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    private val _activeEngineLabel = MutableStateFlow("Motor padrão do aparelho")
    val activeEngineLabel: StateFlow<String> = _activeEngineLabel.asStateFlow()

    private val _hasGoogleTtsInstalled = MutableStateFlow(false)
    val hasGoogleTtsInstalled: StateFlow<Boolean> = _hasGoogleTtsInstalled.asStateFlow()

    private val _isPlayingSample = MutableStateFlow(false)
    val isPlayingSample: StateFlow<Boolean> = _isPlayingSample.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val mediaAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private var focusRequest: AudioFocusRequest? = null

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (_playbackState.value.isActiveContinuous ||
                    _playbackState.value.mode == PlaybackMode.SPEAKING_SINGLE ||
                    _isPlayingSample.value
                ) {
                    Log.i(TAG, "Foco de áudio perdido ($change); pausando leitura")
                    pauseReading(note = "Leitura pausada por interrupção de áudio do sistema.")
                    onAudioFocusInterrupted()
                }
            }
        }
    }

    init {
        _hasGoogleTtsInstalled.value = isGoogleTtsAvailable()
        initializeEngine()
    }

    fun initializeEngine(forceDefaultEngine: Boolean = false) {
        releaseInternalEngine()
        _availability.value = TtsAvailability.INITIALIZING
        val googleInstalled = isGoogleTtsAvailable()
        _hasGoogleTtsInstalled.value = googleInstalled
        val useGoogle = !forceDefaultEngine && preferGoogleTtsEngine && googleInstalled
        currentlyUsingGoogleEngine = useGoogle
        fallbackToDefaultAttempted = forceDefaultEngine
        _activeEngineLabel.value = if (useGoogle) {
            "Serviços de fala do Google"
        } else {
            "Motor padrão do aparelho"
        }
        tts = if (useGoogle) {
            TextToSpeech(appContext, this, GOOGLE_TTS_PACKAGE)
        } else {
            TextToSpeech(appContext, this)
        }
    }

    private fun isGoogleTtsAvailable(): Boolean {
        return try {
            val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
            val services = appContext.packageManager.queryIntentServices(intent, 0)
            services.any { it.serviceInfo?.packageName == GOOGLE_TTS_PACKAGE }
        } catch (_: Exception) {
            false
        }
    }

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            if (currentlyUsingGoogleEngine && !fallbackToDefaultAttempted) {
                Log.w(TAG, "Google TTS falhou (status=$status); tentando motor padrão do sistema")
                initializeEngine(forceDefaultEngine = true)
                return
            }
            Log.e(TAG, "Falha ao inicializar motor TextToSpeech (status=$status)")
            _availability.value = TtsAvailability.ENGINE_ERROR
            return
        }

        val resolvedPkg = if (currentlyUsingGoogleEngine) GOOGLE_TTS_PACKAGE else engine.defaultEngine
        _activeEngineLabel.value = when (resolvedPkg) {
            GOOGLE_TTS_PACKAGE -> "Serviços de fala do Google"
            "com.samsung.SMT" -> "Samsung Text-to-Speech"
            else -> "Motor padrão do aparelho"
        }

        engine.setAudioAttributes(mediaAudioAttributes)
        engine.setSpeechRate(speechRate)
        engine.setPitch(speechPitch)
        engine.setOnUtteranceProgressListener(utteranceListener)

        refreshVoicesInternal()
    }

    fun refreshVoices() {
        if (tts == null || _availability.value == TtsAvailability.ENGINE_ERROR) {
            initializeEngine()
        } else {
            refreshVoicesInternal()
        }
    }

    private fun refreshVoicesInternal() {
        val engine = tts ?: return
        val ptBrLocale = Locale("pt", "BR")
        val langResult = engine.isLanguageAvailable(ptBrLocale)

        val allVoices = try {
            engine.voices.orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível listar vozes do motor TTS: ${e.javaClass.simpleName}")
            emptySet()
        }

        val ptBrVoices = allVoices
            .filter { v ->
                val loc = v.locale
                loc != null &&
                    loc.language.equals("pt", ignoreCase = true) &&
                    loc.country.equals("BR", ignoreCase = true) &&
                    !v.isNotInstalledFlag()
            }
            .sortedWith(
                compareByDescending<Voice> { voiceNaturalnessScore(it) }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenByDescending { it.quality }
                    .thenBy { it.name }
            )

        voiceMap = ptBrVoices.associateBy { it.name }

        val mappedOptions = ptBrVoices.mapIndexed { idx, voice ->
            val isHighOrNeural = isHighQualityOrNeuralVoice(voice)
            val networkTag = if (voice.isNetworkConnectionRequired) "Online" else "Offline"
            val qualityBadge = when {
                voice.isNetworkConnectionRequired && isHighOrNeural -> "Neural • Mais natural"
                !voice.isNetworkConnectionRequired && voice.quality >= Voice.QUALITY_HIGH -> "Alta qualidade • Offline"
                voice.isNetworkConnectionRequired -> "Online"
                else -> "Compacta • Offline"
            }
            VoiceOption(
                name = voice.name,
                displayName = formatFriendlyVoiceName(voice, idx + 1, networkTag, isHighOrNeural),
                localeTag = voice.locale.toLanguageTag(),
                requiresNetwork = voice.isNetworkConnectionRequired,
                quality = voice.quality,
                isNaturalRecommended = isHighOrNeural,
                qualityBadge = qualityBadge,
            )
        }
        _voices.value = mappedOptions

        val ptBrSupported = langResult >= TextToSpeech.LANG_AVAILABLE || mappedOptions.isNotEmpty()
        if (!ptBrSupported) {
            _availability.value = TtsAvailability.PT_BR_MISSING
            return
        }

        engine.language = ptBrLocale
        _availability.value = TtsAvailability.READY

        // Restore preferred voice if still available, else select the most natural pt-BR voice
        val targetName = preferredVoiceName?.takeIf { voiceMap.containsKey(it) }
            ?: mappedOptions.firstOrNull()?.name

        if (targetName != null) {
            applyVoice(targetName)
        }
    }

    private fun isHighQualityOrNeuralVoice(voice: Voice): Boolean {
        val lower = voice.name.lowercase(Locale.ROOT)
        return voice.quality >= Voice.QUALITY_HIGH ||
            lower.contains("network") ||
            lower.contains("neural") ||
            lower.contains("wavenet")
    }

    private fun voiceNaturalnessScore(voice: Voice): Int {
        val lower = voice.name.lowercase(Locale.ROOT)
        var score = voice.quality
        if (lower.contains("neural") || lower.contains("wavenet")) score += 250
        if (voice.isNetworkConnectionRequired && voice.quality >= Voice.QUALITY_HIGH) score += 120
        // High-quality local neural voice is ideal (natural + works offline)
        if (!voice.isNetworkConnectionRequired && voice.quality >= Voice.QUALITY_HIGH) score += 180
        // Prefer standard modern Google pt-BR voices over legacy compact ones
        if (lower.contains("ptd") || lower.contains("pte") || lower.contains("ptb")) score += 20
        return score
    }

    private fun Voice.isNotInstalledFlag(): Boolean {
        val features = try {
            this.features.orEmpty()
        } catch (_: Exception) {
            emptySet()
        }
        return features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
    }

    private fun formatFriendlyVoiceName(
        voice: Voice,
        index: Int,
        networkTag: String,
        isHighOrNeural: Boolean,
    ): String {
        val raw = voice.name
        val tokens = raw.split('-', '_').filter { it.isNotBlank() }
        val variantCode = tokens.firstOrNull { token ->
            token.length == 3 &&
                !token.equals("local", ignoreCase = true) &&
                !token.equals("bra", ignoreCase = true) &&
                !token.equals("net", ignoreCase = true)
        }?.uppercase(Locale.ROOT)
        val prefix = if (isHighOrNeural) "Narrador pt-BR $index" else "Voz pt-BR $index"
        return if (variantCode != null) {
            "$prefix ($variantCode • $networkTag)"
        } else {
            "$prefix ($networkTag)"
        }
    }

    fun updateSettings(
        voiceName: String?,
        rate: Float,
        pitch: Float,
        pauseSec: Float,
        naturalProsody: Boolean,
        preferGoogleEngine: Boolean,
    ) {
        val enginePreferenceChanged = preferGoogleTtsEngine != preferGoogleEngine
        preferredVoiceName = voiceName
        speechRate = rate.coerceIn(0.6f, 1.3f)
        speechPitch = pitch.coerceIn(0.75f, 1.2f)
        pauseSeconds = pauseSec.coerceIn(0f, 5.0f)
        naturalProsodyEnabled = naturalProsody
        preferGoogleTtsEngine = preferGoogleEngine

        if (enginePreferenceChanged && _hasGoogleTtsInstalled.value) {
            initializeEngine()
            return
        }

        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)
        if (voiceName != null && voiceMap.containsKey(voiceName)) {
            applyVoice(voiceName)
        }
    }

    fun readSystemMediaVolumeNormalized(): Float {
        return try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, max)
            (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        } catch (_: Exception) {
            returnVolume
        }
    }

    /**
     * Adjusts return audio volume in real time via STREAM_MUSIC + ultra-quiet TTS gain attenuation.
     * Safe to call even mid-recording or mid-utterance without interrupting video or speech.
     */
    fun setReturnVolume(volume: Float, applyToSystemStream: Boolean = true) {
        val clamped = volume.coerceIn(0f, 1f)
        returnVolume = clamped

        if (clamped <= 0.005f) {
            effectiveTtsGain = 0f
            if (applyToSystemStream) {
                trySetStreamVolume(0)
            }
            return
        }

        val maxSteps = try {
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        } catch (_: Exception) {
            15
        }

        // For ultra-low levels (1%..15%), keep hardware stream at step 1 and smoothly attenuate TTS gain
        // so Bluetooth earbuds can stay whisper-quiet without leaking into the microphone.
        if (clamped < 0.15f) {
            effectiveTtsGain = (clamped / 0.15f).coerceIn(0.12f, 1f)
            if (applyToSystemStream) {
                trySetStreamVolume(1.coerceAtMost(maxSteps))
            }
        } else {
            effectiveTtsGain = 1.0f
            if (applyToSystemStream) {
                val targetStep = (clamped * maxSteps).roundToInt().coerceIn(1, maxSteps)
                trySetStreamVolume(targetStep)
            }
        }
    }

    private fun trySetStreamVolume(targetStep: Int) {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetStep, 0)
        } catch (e: SecurityException) {
            // Do-Not-Disturb policy on some OEMs can block stream writes; fall back to TTS gain
            Log.w(TAG, "Sem permissão para alterar volume do sistema; usando atenuação interna")
            effectiveTtsGain = returnVolume.coerceIn(0f, 1f)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao ajustar volume do sistema: ${e.javaClass.simpleName}")
        }
    }

    private fun applyVoice(voiceName: String) {
        val voice = voiceMap[voiceName] ?: return
        try {
            val res = tts?.setVoice(voice)
            if (res == TextToSpeech.SUCCESS) {
                _selectedVoiceName.value = voiceName
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao aplicar voz selecionada: ${e.javaClass.simpleName}")
        }
    }

    fun setSentences(sentences: List<String>, preserveIndex: Boolean = true) {
        currentSentences = sentences
        val currentIdx = _playbackState.value.currentIndex
        val clamped = if (sentences.isEmpty()) 0 else {
            if (preserveIndex) currentIdx.coerceIn(0, sentences.lastIndex) else 0
        }
        _playbackState.value = _playbackState.value.copy(currentIndex = clamped)
    }

    fun toggleSample(sampleText: String) {
        if (_isPlayingSample.value) {
            stopSample()
            return
        }
        if (_availability.value != TtsAvailability.READY) return

        cancelPendingTasksAndTts()
        if (!requestAudioFocus()) {
            _playbackState.value = _playbackState.value.copy(
                errorMessage = "Não foi possível obter foco de áudio para a amostra."
            )
            return
        }

        val gen = generationCounter.incrementAndGet()
        val id = "sample_$gen"
        activeUtteranceId = id
        _isPlayingSample.value = true

        val result = speakInternal(sampleText, id)
        if (result != TextToSpeech.SUCCESS) {
            _isPlayingSample.value = false
            activeUtteranceId = null
            abandonAudioFocus()
            _playbackState.value = _playbackState.value.copy(
                errorMessage = "O motor de voz não conseguiu reproduzir a amostra."
            )
        }
    }

    fun stopSample() {
        if (!_isPlayingSample.value) return
        generationCounter.incrementAndGet()
        activeUtteranceId = null
        _isPlayingSample.value = false
        tts?.stop()
        abandonAudioFocus()
    }

    /**
     * Starts continuous reading from the given index (or current selected index).
     */
    fun startContinuous(fromIndex: Int = _playbackState.value.currentIndex, isRehearsal: Boolean) {
        if (currentSentences.isEmpty()) return
        if (_availability.value != TtsAvailability.READY) {
            _playbackState.value = _playbackState.value.copy(
                errorMessage = "Voz em português indisponível no momento."
            )
            return
        }
        val safeIdx = fromIndex.coerceIn(0, currentSentences.lastIndex)
        speakPhrase(index = safeIdx, continuous = true, isRehearsal = isRehearsal)
    }

    /**
     * Toggles continuous reading (Iniciar / Pausar leitura).
     * Resuming always restarts the currently selected phrase from the beginning.
     */
    fun togglePlayPause(isRehearsal: Boolean) {
        val state = _playbackState.value
        if (state.isActiveContinuous || state.mode == PlaybackMode.SPEAKING_SINGLE) {
            pauseReading()
        } else {
            val targetIdx = if (state.mode == PlaybackMode.COMPLETED) 0 else state.currentIndex
            startContinuous(fromIndex = targetIdx, isRehearsal = isRehearsal)
        }
    }

    /**
     * Exact specification behavior when tapping a phrase:
     * - Cancels current audio & pending advance immediately.
     * - Selects the tapped phrase.
     * - If continuous reading was active, plays it and continues to next phrases after done + pause.
     * - If reading was paused/idle/completed, plays ONLY this single phrase and returns to PAUSED.
     */
    fun onPhraseTapped(index: Int, isRehearsal: Boolean) {
        if (index !in currentSentences.indices) return
        val wasContinuous = _playbackState.value.isActiveContinuous
        speakPhrase(index = index, continuous = wasContinuous, isRehearsal = isRehearsal)
    }

    fun repeatCurrentPhrase(isRehearsal: Boolean) {
        if (currentSentences.isEmpty()) return
        val idx = _playbackState.value.currentIndex.coerceIn(0, currentSentences.lastIndex)
        val wasContinuous = _playbackState.value.isActiveContinuous
        speakPhrase(index = idx, continuous = wasContinuous, isRehearsal = isRehearsal)
    }

    fun previousPhrase(isRehearsal: Boolean) {
        if (currentSentences.isEmpty()) return
        val prevIdx = (_playbackState.value.currentIndex - 1).coerceAtLeast(0)
        val wasContinuous = _playbackState.value.isActiveContinuous
        speakPhrase(index = prevIdx, continuous = wasContinuous, isRehearsal = isRehearsal)
    }

    fun nextPhrase(isRehearsal: Boolean) {
        if (currentSentences.isEmpty()) return
        val nextIdx = (_playbackState.value.currentIndex + 1).coerceAtMost(currentSentences.lastIndex)
        val wasContinuous = _playbackState.value.isActiveContinuous
        speakPhrase(index = nextIdx, continuous = wasContinuous, isRehearsal = isRehearsal)
    }

    fun selectIndexWithoutSpeaking(index: Int) {
        if (currentSentences.isEmpty()) return
        val clamped = index.coerceIn(0, currentSentences.lastIndex)
        _playbackState.value = _playbackState.value.copy(currentIndex = clamped)
    }

    fun pauseReading(note: String? = null) {
        cancelPendingTasksAndTts()
        abandonAudioFocus()
        val prev = _playbackState.value
        val nextMode = if (prev.mode == PlaybackMode.IDLE) PlaybackMode.IDLE else PlaybackMode.PAUSED
        _playbackState.value = prev.copy(
            mode = nextMode,
            statusNote = note,
        )
    }

    /**
     * Cancels rehearsal/reading cleanly when starting or finalizing a recording.
     * Preserves the selected phrase index so recording begins from the selected phrase.
     */
    fun stopAllAndResetToIdle(keepSelectedIndex: Boolean = true) {
        cancelPendingTasksAndTts()
        abandonAudioFocus()
        val idx = if (keepSelectedIndex) _playbackState.value.currentIndex else 0
        _playbackState.value = PlaybackState(
            mode = PlaybackMode.IDLE,
            currentIndex = idx.coerceIn(0, (currentSentences.size - 1).coerceAtLeast(0)),
            isRehearsal = false,
            statusNote = null,
            errorMessage = null,
        )
    }

    fun clearError() {
        _playbackState.value = _playbackState.value.copy(errorMessage = null)
    }

    fun clearStatusNote() {
        _playbackState.value = _playbackState.value.copy(statusNote = null)
    }

    private fun speakPhrase(index: Int, continuous: Boolean, isRehearsal: Boolean) {
        if (index !in currentSentences.indices) return
        if (_availability.value != TtsAvailability.READY) {
            _playbackState.value = _playbackState.value.copy(
                currentIndex = index,
                mode = PlaybackMode.PAUSED,
                errorMessage = "Motor de voz em pt-BR não está pronto.",
            )
            return
        }

        cancelPendingTasksAndTts()

        if (!requestAudioFocus()) {
            _playbackState.value = _playbackState.value.copy(
                currentIndex = index,
                mode = PlaybackMode.PAUSED,
                errorMessage = "Áudio ocupado por outro aplicativo ou chamada.",
            )
            return
        }

        val gen = generationCounter.incrementAndGet()
        val utteranceId = "eg_${gen}_${index}_${System.nanoTime()}"
        activeUtteranceId = utteranceId

        _playbackState.value = PlaybackState(
            mode = if (continuous) PlaybackMode.SPEAKING_CONTINUOUS else PlaybackMode.SPEAKING_SINGLE,
            currentIndex = index,
            isRehearsal = isRehearsal,
            statusNote = null,
            errorMessage = null,
        )

        val phraseText = currentSentences[index]
        val status = speakInternal(phraseText, utteranceId)
        if (status != TextToSpeech.SUCCESS) {
            activeUtteranceId = null
            abandonAudioFocus()
            _playbackState.value = _playbackState.value.copy(
                mode = PlaybackMode.PAUSED,
                errorMessage = "Falha ao reproduzir a frase ${index + 1}. Verifique a voz selecionada.",
            )
        }
    }

    private fun speakInternal(text: String, utteranceId: String): Int {
        val engine = tts ?: return TextToSpeech.ERROR
        engine.setSpeechRate(speechRate)
        engine.setPitch(speechPitch)
        val spokenText = NarratorProsodyFormatter.formatForNarrator(text, naturalProsodyEnabled)
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, effectiveTtsGain.coerceIn(0f, 1f))
        }
        return engine.speak(spokenText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            // Active utterance confirmed started
        }

        override fun onDone(utteranceId: String?) {
            scope.launch {
                if (utteranceId == null || utteranceId != activeUtteranceId) return@launch

                if (utteranceId.startsWith("sample_")) {
                    activeUtteranceId = null
                    _isPlayingSample.value = false
                    abandonAudioFocus()
                    return@launch
                }

                val state = _playbackState.value
                val expectedGen = generationCounter.get()
                activeUtteranceId = null

                when (state.mode) {
                    PlaybackMode.SPEAKING_SINGLE -> {
                        abandonAudioFocus()
                        _playbackState.value = state.copy(mode = PlaybackMode.PAUSED)
                    }

                    PlaybackMode.SPEAKING_CONTINUOUS -> {
                        val nextIdx = state.currentIndex + 1
                        if (nextIdx >= currentSentences.size) {
                            abandonAudioFocus()
                            _playbackState.value = state.copy(
                                mode = PlaybackMode.COMPLETED,
                                statusNote = "Leitura concluída",
                            )
                        } else {
                            val delayMs = (pauseSeconds * 1000f).toLong().coerceAtLeast(0L)
                            if (delayMs > 0L) {
                                _playbackState.value = state.copy(mode = PlaybackMode.WAITING_PAUSE)
                                pauseDelayJob?.cancel()
                                pauseDelayJob = scope.launch {
                                    delay(delayMs)
                                    // Verify no newer tap/pause invalidated this generation
                                    if (generationCounter.get() == expectedGen &&
                                        _playbackState.value.mode == PlaybackMode.WAITING_PAUSE
                                    ) {
                                        speakPhrase(
                                            index = nextIdx,
                                            continuous = true,
                                            isRehearsal = state.isRehearsal,
                                        )
                                    }
                                }
                            } else {
                                if (generationCounter.get() == expectedGen) {
                                    speakPhrase(
                                        index = nextIdx,
                                        continuous = true,
                                        isRehearsal = state.isRehearsal,
                                    )
                                }
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            handleUtteranceError(utteranceId, -1)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            handleUtteranceError(utteranceId, errorCode)
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            // Intentional stop/flush — state already updated by the caller
        }
    }

    private fun handleUtteranceError(utteranceId: String?, errorCode: Int) {
        scope.launch {
            if (utteranceId == null || utteranceId != activeUtteranceId) return@launch
            activeUtteranceId = null
            _isPlayingSample.value = false
            abandonAudioFocus()

            val detail = when (errorCode) {
                TextToSpeech.ERROR_NETWORK,
                TextToSpeech.ERROR_NETWORK_TIMEOUT -> "A voz selecionada exige conexão com a internet. Escolha uma voz offline para usar sem rede."
                TextToSpeech.ERROR_NOT_INSTALLED_YET -> "Os dados da voz selecionada ainda não foram instalados."
                else -> "Erro ao reproduzir voz guia (código $errorCode). Tente outra voz."
            }
            Log.w(TAG, "Erro TTS na reprodução (code=$errorCode)")
            _playbackState.value = _playbackState.value.copy(
                mode = PlaybackMode.PAUSED,
                errorMessage = detail,
            )
        }
    }

    private fun cancelPendingTasksAndTts() {
        generationCounter.incrementAndGet()
        activeUtteranceId = null
        pauseDelayJob?.cancel()
        pauseDelayJob = null
        _isPlayingSample.value = false
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    private fun requestAudioFocus(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(mediaAudioAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build()
                focusRequest = req
                audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao solicitar foco de áudio: ${e.javaClass.simpleName}")
            true
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusListener)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseInternalEngine() {
        cancelPendingTasksAndTts()
        abandonAudioFocus()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
    }

    fun release() {
        releaseInternalEngine()
    }

    companion object {
        private const val TAG = "TeleprompterTtsEngine"
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
    }
}
