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
)

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
    val speechRate: Float = 0.9f,
    val pauseSeconds: Float = 1.0f,
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
