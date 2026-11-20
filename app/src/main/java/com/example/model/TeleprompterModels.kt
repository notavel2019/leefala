package com.example.model

import java.util.UUID

data class ScriptDocument(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val rawText: String = "",
    val sentences: List<String> = emptyList(),
    val hasManualSplitEdits: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

data class VoiceOption(
    val name: String,
    val displayName: String,
    val localeTag: String,
    val requiresNetwork: Boolean,
    val quality: Int,
    val isNaturalRecommended: Boolean = false,
    val qualityBadge: String = "",
)

enum class NarratorStyle(
    val label: String,
    val subtitle: String,
    val speechRate: Float,
    val speechPitch: Float,
    val pauseSeconds: Float,
) {
    NARRATOR_FLUID(
        label = "Narrador fluido",
        subtitle = "Tom caloroso e cadência natural",
        speechRate = 1.0f,
        speechPitch = 0.94f,
        pauseSeconds = 0.8f,
    ),
    ANNOUNCER_DEEP(
        label = "Locutor grave",
        subtitle = "Mais firme e envolvente",
        speechRate = 0.95f,
        speechPitch = 0.86f,
        pauseSeconds = 1.0f,
    ),
    CONVERSATIONAL(
        label = "Conversa leve",
        subtitle = "Ágil e espontâneo",
        speechRate = 1.05f,
        speechPitch = 1.02f,
        pauseSeconds = 0.5f,
    ),
    DIDACTIC(
        label = "Didático",
        subtitle = "Calmo e bem articulado",
        speechRate = 0.88f,
        speechPitch = 0.96f,
        pauseSeconds = 1.5f,
    ),
    CUSTOM(
        label = "Personalizado",
        subtitle = "Ajuste manual",
        speechRate = 1.0f,
        speechPitch = 0.94f,
        pauseSeconds = 1.0f,
    ),
}

enum class AudioRouteKind {
    BLUETOOTH_MEDIA,
    BLUETOOTH_CALL_SCO,
    WIRED_HEADSET,
    PHONE_SPEAKER,
    UNCONFIRMED,
}

data class AudioRouteInfo(
    val kind: AudioRouteKind = AudioRouteKind.UNCONFIRMED,
    val deviceName: String = "",
    val isHeadsetSafe: Boolean = false,
    val description: String = "Rota de áudio não confirmada",
)

data class TeleprompterPreferences(
    val selectedVoiceName: String? = null,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 0.94f,
    val pauseSeconds: Float = 1.0f,
    val narratorStyle: NarratorStyle = NarratorStyle.NARRATOR_FLUID,
    val naturalProsodyEnabled: Boolean = true,
    val preferGoogleTtsEngine: Boolean = true,
    val returnVolume: Float = 0.75f,
    val overlayFontSizeSp: Float = 19f,
    val panelHeightMode: PanelHeightMode = PanelHeightMode.MEDIUM,
    val preferFrontCamera: Boolean = true,
)

enum class PanelHeightMode(val label: String, val maxFraction: Float) {
    COMPACT("Compacto", 0.24f),
    MEDIUM("Médio", 0.36f),
    LARGE("Amplo", 0.50f);

    fun next(): PanelHeightMode = when (this) {
        COMPACT -> MEDIUM
        MEDIUM -> LARGE
        LARGE -> COMPACT
    }
}

enum class TtsAvailability {
    INITIALIZING,
    READY,
    PT_BR_MISSING,
    ENGINE_ERROR,
}

enum class PlaybackMode {
    IDLE,
    SPEAKING_CONTINUOUS,
    WAITING_PAUSE,
    SPEAKING_SINGLE,
    PAUSED,
    COMPLETED,
}

data class PlaybackState(
    val mode: PlaybackMode = PlaybackMode.IDLE,
    val currentIndex: Int = 0,
    val isRehearsal: Boolean = false,
    val statusNote: String? = null,
    val errorMessage: String? = null,
) {
    val isActiveContinuous: Boolean
        get() = mode == PlaybackMode.SPEAKING_CONTINUOUS || mode == PlaybackMode.WAITING_PAUSE

    val isSpeakingNow: Boolean
        get() = mode == PlaybackMode.SPEAKING_CONTINUOUS || mode == PlaybackMode.SPEAKING_SINGLE
}

sealed class RecordingState {
    data object Idle : RecordingState()
    data class Starting(val isShortTest: Boolean = false) : RecordingState()
    data class Countdown(val secondsRemaining: Int, val isShortTest: Boolean = false) : RecordingState()
    data class Recording(
        val elapsedMillis: Long = 0L,
        val audioAmplitude: Double = 0.0,
        val hasAudioTrack: Boolean = true,
        val isShortTest: Boolean = false,
    ) : RecordingState()

    data class Finalizing(val isShortTest: Boolean = false) : RecordingState()
    data class Saved(
        val uriString: String,
        val displayName: String,
        val durationMillis: Long,
        val qualityLabel: String,
        val isShortTest: Boolean = false,
    ) : RecordingState()

    data class Error(val message: String) : RecordingState()

    val isSessionLocked: Boolean
        get() = this is Starting || this is Countdown || this is Recording || this is Finalizing
}

enum class AppScreen {
    SETUP,
    STUDIO,
}
