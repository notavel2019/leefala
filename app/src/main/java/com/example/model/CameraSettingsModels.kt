package com.example.model

import android.hardware.camera2.CaptureRequest

data class CameraLensOption(
    val cameraId: String,
    val label: String,
    val isFront: Boolean,
    val focalLengthMm: Float?,
    val apertures: List<Float>,
)

enum class WhiteBalancePreset(val label: String, val camera2Mode: Int) {
    AUTO("Auto", CaptureRequest.CONTROL_AWB_MODE_AUTO),
    DAYLIGHT("Luz do dia", CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT),
    CLOUDY("Nublado", CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    INCANDESCENT("Incandescente", CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT("Fluorescente", CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT),
    SHADE("Sombra", CaptureRequest.CONTROL_AWB_MODE_SHADE),
}

data class CameraHardwareCapabilities(
    val availableLenses: List<CameraLensOption> = emptyList(),
    val activeLens: CameraLensOption? = null,
    val availableApertures: List<Float> = emptyList(),
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val exposureMinIndex: Int = 0,
    val exposureMaxIndex: Int = 0,
    val exposureStepEv: Float = 0.33f,
    val supportsManualExposure: Boolean = false,
    val isoMin: Int = 100,
    val isoMax: Int = 1600,
    val shutterMinNs: Long = 1_000_000L,
    val shutterMaxNs: Long = 33_333_333L,
    val supportedWhiteBalance: List<WhiteBalancePreset> = listOf(WhiteBalancePreset.AUTO),
    val supportedQualities: List<String> = emptyList(),
    val supportedFps: List<Int> = listOf(30),
    val supportsVideoStabilization: Boolean = false,
    val hasFlashUnit: Boolean = false,
) {
    val supportsExposureCompensation: Boolean
        get() = exposureMaxIndex > exposureMinIndex

    val supportsVariableAperture: Boolean
        get() = availableApertures.size > 1
}

data class CameraSettingsState(
    val selectedCameraId: String? = null,
    val selectedQualityLabel: String? = null,
    val selectedFps: Int = 30,
    val videoStabilizationEnabled: Boolean = true,
    val zoomRatio: Float = 1.0f,
    val exposureIndex: Int = 0,
    val manualExposureEnabled: Boolean = false,
    val manualIso: Int = 200,
    val manualShutterNs: Long = 16_666_666L, // 1/60s
    val selectedAperture: Float? = null,
    val whiteBalance: WhiteBalancePreset = WhiteBalancePreset.AUTO,
    val torchEnabled: Boolean = false,
    val aeLocked: Boolean = false,
)

sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data class UpdateAvailable(
        val latestVersion: String,
        val currentVersion: String,
        val downloadUrl: String,
        val releasePageUrl: String,
        val notes: String,
    ) : UpdateCheckState()

    data class UpToDate(
        val currentVersion: String,
        val releasePageUrl: String,
    ) : UpdateCheckState()

    data class NeedsBrowserAccess(
        val message: String,
        val releasesUrl: String,
        val actionsUrl: String,
    ) : UpdateCheckState()
}
