package com.example.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.CamcorderProfile
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.view.Surface
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.CameraHardwareCapabilities
import com.example.model.CameraLensOption
import com.example.model.CameraSettingsState
import com.example.model.WhiteBalancePreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class CameraBindResult(
    val effectiveQualityLabel: String,
    val isFrontCamera: Boolean,
    val hasOtherCamera: Boolean,
    val videoCaptureSupported: Boolean = true,
    val warningMessage: String? = null,
    val capabilities: CameraHardwareCapabilities = CameraHardwareCapabilities(),
    val boundCameraId: String? = null,
)

/**
 * Real CameraX 1.5.0 + Camera2 Interop video capture controller.
 * - Enumerates real physical/logical lenses, apertures (f/), focal lengths, zoom range,
 *   exposure compensation (EV), manual ISO/shutter, white balance, FPS, and video stabilization.
 * - Portrait vertical output with MirrorMode.MIRROR_MODE_OFF (keeps physical scene text readable).
 * - Pre-checks Android CamcorderProfile / EncoderProfiles via CameraManager before instantiating
 *   CameraX Recorder/VideoCapture so virtual/emulator cameras without encoder profiles gracefully
 *   bind Preview-only without triggering CapabilitiesByQuality errors.
 * - Saves directly to MediaStore Movies/EscutaEGrava and only confirms success on Finalize with no error.
 */
@OptIn(ExperimentalCamera2Interop::class)
class CameraRecorderController(context: Context) {

    private val appContext = context.applicationContext
    private var cameraProvider: ProcessCameraProvider? = null
    private var boundCamera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var lastEffectiveQualityLabel: String = "1080p"

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean,
        settings: CameraSettingsState,
        onBound: (CameraBindResult) -> Unit,
        onError: (String) -> Unit,
    ) {
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener(
            {
                try {
                    val provider = future.get()
                    cameraProvider = provider

                    val allLenses = enumerateLenses(provider)
                    val hasFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                    val hasBack = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)

                    // Pick exact requested lens if specified and available, else match front/back preference
                    var chosenLens = settings.selectedCameraId?.let { targetId ->
                        allLenses.firstOrNull { it.cameraId == targetId }
                    } ?: allLenses.firstOrNull { it.isFront == useFrontCamera }
                        ?: allLenses.firstOrNull()

                    var actualIsFront = chosenLens?.isFront ?: when {
                        useFrontCamera && hasFront -> true
                        !useFrontCamera && hasBack -> false
                        hasFront -> true
                        hasBack -> false
                        else -> {
                            onError("Nenhuma câmera disponível neste aparelho.")
                            return@addListener
                        }
                    }

                    // Virtual Android / Cuttlefish emulators lack valid hardware EncoderProfiles and log
                    // repeated CapabilitiesByQuality/MediaProfiles errors if VideoCapture is bound.
                    // Bind Preview-only cleanly on emulators; real physical devices bind full VideoCapture.
                    val isEmulator = isAndroidEmulator()

                    // Pre-check real EncoderProfiles via CameraManager/CamcorderProfile before touching Recorder
                    var supportedQualities = if (isEmulator) {
                        emptyList()
                    } else {
                        queryEncoderQualitiesForCameraId(
                            chosenLens?.cameraId ?: findCameraIdForFacing(actualIsFront)
                        )
                    }

                    // If chosen lens has no video encoder profiles, check if another lens does
                    if (!isEmulator && supportedQualities.isEmpty() && settings.selectedCameraId == null && allLenses.size > 1) {
                        val altLens = allLenses.firstOrNull { candidate ->
                            candidate.cameraId != chosenLens?.cameraId &&
                                queryEncoderQualitiesForCameraId(candidate.cameraId).isNotEmpty()
                        }
                        if (altLens != null) {
                            chosenLens = altLens
                            actualIsFront = altLens.isFront
                            supportedQualities = queryEncoderQualitiesForCameraId(altLens.cameraId)
                        }
                    }

                    val selector = buildSelectorForLens(chosenLens, actualIsFront)

                    val preview = Preview.Builder()
                        .setTargetRotation(Surface.ROTATION_0)
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }

                    provider.unbindAll()

                    if (supportedQualities.isEmpty()) {
                        val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                        boundCamera = camera
                        videoCapture = null
                        lastEffectiveQualityLabel = "Somente prévia"
                        val caps = readHardwareCapabilities(
                            camera = camera,
                            allLenses = allLenses,
                            activeLens = chosenLens,
                            supportedQualities = emptyList(),
                        )
                        applyLiveControls(settings, caps)
                        onBound(
                            CameraBindResult(
                                effectiveQualityLabel = "Somente prévia",
                                isFrontCamera = actualIsFront,
                                hasOtherCamera = allLenses.size > 1 || (hasFront && hasBack),
                                videoCaptureSupported = false,
                                warningMessage = if (isEmulator) null else {
                                    "Prévia da câmera ativa, mas esta lente não informou perfis de gravação de vídeo."
                                },
                                capabilities = caps,
                                boundCameraId = chosenLens?.cameraId,
                            )
                        )
                        return@addListener
                    }

                    // Honor user's chosen resolution if supported on this lens; else default to 1080p -> 720p -> 480p
                    val qualityLabels = supportedQualities.map { qualityToLabel(it) }
                    val orderedForSelector = settings.selectedQualityLabel?.let { wanted ->
                        val match = supportedQualities.firstOrNull { qualityToLabel(it) == wanted }
                        if (match != null) {
                            listOf(match) + supportedQualities.filterNot { it == match }
                        } else {
                            supportedQualities
                        }
                    } ?: supportedQualities

                    val resolvedQuality = orderedForSelector.first()
                    lastEffectiveQualityLabel = qualityToLabel(resolvedQuality)

                    val qualitySelector = QualitySelector.fromOrderedList(
                        orderedForSelector,
                        FallbackStrategy.lowerQualityOrHigherThan(resolvedQuality),
                    )

                    val recorder = Recorder.Builder()
                        .setQualitySelector(qualitySelector)
                        .build()

                    val vcBuilder = VideoCapture.Builder(recorder)
                        .setDynamicRange(DynamicRange.SDR)
                        .setMirrorMode(MirrorMode.MIRROR_MODE_OFF)
                        .setTargetRotation(Surface.ROTATION_0)

                    // Apply hardware video stabilization & target FPS only if supported by this camera's characteristics
                    val activeCamId = chosenLens?.cameraId ?: findCameraIdForFacing(actualIsFront)
                    val preCapsFps = querySupportedFpsForCameraId(activeCamId)
                    val preCapsStab = querySupportsVideoStabilization(activeCamId)
                    if (preCapsStab) {
                        vcBuilder.setVideoStabilizationEnabled(settings.videoStabilizationEnabled)
                    }
                    if (settings.selectedFps in preCapsFps) {
                        vcBuilder.setTargetFrameRate(Range(settings.selectedFps, settings.selectedFps))
                    }

                    val vc = vcBuilder.build()

                    try {
                        val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
                        boundCamera = camera
                        videoCapture = vc
                        val caps = readHardwareCapabilities(
                            camera = camera,
                            allLenses = allLenses,
                            activeLens = chosenLens,
                            supportedQualities = qualityLabels,
                        )
                        applyLiveControls(settings, caps)
                        onBound(
                            CameraBindResult(
                                effectiveQualityLabel = lastEffectiveQualityLabel,
                                isFrontCamera = actualIsFront,
                                hasOtherCamera = allLenses.size > 1 || (hasFront && hasBack),
                                videoCaptureSupported = true,
                                capabilities = caps,
                                boundCameraId = chosenLens?.cameraId,
                            )
                        )
                    } catch (bindEx: Exception) {
                        Log.w(TAG, "VideoCapture indisponível nesta combinação (${bindEx.javaClass.simpleName}); mantendo prévia")
                        provider.unbindAll()
                        val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                        boundCamera = camera
                        videoCapture = null
                        lastEffectiveQualityLabel = "Somente prévia"
                        val caps = readHardwareCapabilities(
                            camera = camera,
                            allLenses = allLenses,
                            activeLens = chosenLens,
                            supportedQualities = emptyList(),
                        )
                        applyLiveControls(settings, caps)
                        onBound(
                            CameraBindResult(
                                effectiveQualityLabel = "Somente prévia",
                                isFrontCamera = actualIsFront,
                                hasOtherCamera = allLenses.size > 1 || (hasFront && hasBack),
                                videoCaptureSupported = false,
                                warningMessage = "Prévia ativa, mas não foi possível vincular a gravação de vídeo com esta configuração de lente.",
                                capabilities = caps,
                                boundCameraId = chosenLens?.cameraId,
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Falha ao inicializar câmera: ${e.javaClass.simpleName}")
                    onError("Não foi possível abrir a câmera. Verifique se outro aplicativo está usando a câmera.")
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    /**
     * Applies live non-destructive CameraX & Camera2 hardware controls (zoom, aperture, EV,
     * manual ISO/shutter, white balance, AE lock, torch) without interrupting recording.
     */
    fun applyLiveControls(
        settings: CameraSettingsState,
        capabilities: CameraHardwareCapabilities,
    ) {
        val camera = boundCamera ?: return
        try {
            // Zoom ratio
            if (capabilities.maxZoomRatio > capabilities.minZoomRatio) {
                val safeZoom = settings.zoomRatio.coerceIn(
                    capabilities.minZoomRatio,
                    capabilities.maxZoomRatio,
                )
                camera.cameraControl.setZoomRatio(safeZoom)
            }

            // Torch
            if (capabilities.hasFlashUnit) {
                camera.cameraControl.enableTorch(settings.torchEnabled)
            }

            // Exposure compensation (used when Auto Exposure is active)
            if (!settings.manualExposureEnabled && capabilities.supportsExposureCompensation) {
                val safeEv = settings.exposureIndex.coerceIn(
                    capabilities.exposureMinIndex,
                    capabilities.exposureMaxIndex,
                )
                camera.cameraControl.setExposureCompensationIndex(safeEv)
            }

            // Camera2 Interop: Aperture, White Balance, Manual ISO/Shutter, AE Lock
            val optionsBuilder = CaptureRequestOptions.Builder()

            // Variable physical aperture (when hardware supports > 1 aperture value)
            if (capabilities.supportsVariableAperture) {
                val aperture = settings.selectedAperture?.takeIf { it in capabilities.availableApertures }
                    ?: capabilities.availableApertures.firstOrNull()
                if (aperture != null) {
                    optionsBuilder.setCaptureRequestOption(CaptureRequest.LENS_APERTURE, aperture)
                }
            }

            // White balance
            if (settings.whiteBalance in capabilities.supportedWhiteBalance) {
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    settings.whiteBalance.camera2Mode,
                )
            }

            // Manual Exposure (ISO + Shutter) vs Auto Exposure
            if (settings.manualExposureEnabled && capabilities.supportsManualExposure) {
                val safeIso = settings.manualIso.coerceIn(capabilities.isoMin, capabilities.isoMax)
                val safeShutter = settings.manualShutterNs.coerceIn(capabilities.shutterMinNs, capabilities.shutterMaxNs)
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF,
                )
                optionsBuilder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, safeIso)
                optionsBuilder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, safeShutter)
            } else {
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON,
                )
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_LOCK,
                    settings.aeLocked,
                )
            }

            Camera2CameraControl.from(camera.cameraControl)
                .setCaptureRequestOptions(optionsBuilder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Ajuste de câmera não suportado pelo driver: ${e.javaClass.simpleName}")
        }
    }

    fun tapToFocus(previewView: PreviewView, x: Float, y: Float) {
        val camera = boundCamera ?: return
        try {
            val point = previewView.meteringPointFactory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
            )
                .setAutoCancelDuration(4, TimeUnit.SECONDS)
                .build()
            camera.cameraControl.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao focar no ponto tocado: ${e.javaClass.simpleName}")
        }
    }

    private fun enumerateLenses(provider: ProcessCameraProvider): List<CameraLensOption> {
        val manager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val result = mutableListOf<CameraLensOption>()
        var frontCount = 0
        var backCount = 0

        for (info in provider.availableCameraInfos) {
            try {
                val c2Info = Camera2CameraInfo.from(info)
                val camId = c2Info.cameraId
                val chars = manager.getCameraCharacteristics(camId)
                val facing = chars.get(CameraCharacteristics.LENS_FACING) ?: continue
                val isFront = facing == CameraCharacteristics.LENS_FACING_FRONT
                if (facing != CameraCharacteristics.LENS_FACING_FRONT &&
                    facing != CameraCharacteristics.LENS_FACING_BACK
                ) {
                    continue
                }
                val focals = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focalMm = focals?.firstOrNull()
                val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                    ?.toList()
                    ?.distinct()
                    ?.sorted()
                    .orEmpty()

                val baseName = if (isFront) {
                    frontCount++
                    if (frontCount == 1) "Frontal" else "Frontal $frontCount"
                } else {
                    backCount++
                    when {
                        backCount == 1 -> "Traseira"
                        focalMm != null && focalMm < 2.5f -> "Ultra-wide"
                        focalMm != null && focalMm > 7.5f -> "Tele"
                        else -> "Traseira $backCount"
                    }
                }

                val apertureTag = apertures.firstOrNull()?.let { formatAperture(it) }
                val focalTag = focalMm?.let { String.format(Locale.US, "%.1fmm", it) }
                val detail = listOfNotNull(focalTag, apertureTag).joinToString(" • ")
                val fullLabel = if (detail.isNotEmpty()) "$baseName ($detail)" else baseName

                result.add(
                    CameraLensOption(
                        cameraId = camId,
                        label = fullLabel,
                        isFront = isFront,
                        focalLengthMm = focalMm,
                        apertures = apertures,
                    )
                )
            } catch (_: Exception) {
            }
        }
        return result
    }

    private fun buildSelectorForLens(lens: CameraLensOption?, isFront: Boolean): CameraSelector {
        if (lens != null) {
            return CameraSelector.Builder()
                .addCameraFilter { infos ->
                    val exact = infos.filter { Camera2CameraInfo.from(it).cameraId == lens.cameraId }
                    exact.ifEmpty { infos }
                }
                .build()
        }
        return if (isFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    }

    private fun readHardwareCapabilities(
        camera: Camera,
        allLenses: List<CameraLensOption>,
        activeLens: CameraLensOption?,
        supportedQualities: List<String>,
    ): CameraHardwareCapabilities {
        val info = camera.cameraInfo
        val c2Info = Camera2CameraInfo.from(info)
        val zoom = info.zoomState.value
        val exp = info.exposureState

        val apertures = c2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
            ?.toList()
            ?.distinct()
            ?.sorted()
            .orEmpty()

        val aeModes = c2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
            ?.toSet()
            .orEmpty()
        val isoRange = c2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val shutterRange = c2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val supportsManual = aeModes.contains(CaptureRequest.CONTROL_AE_MODE_OFF) &&
            isoRange != null &&
            shutterRange != null

        val awbModes = c2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
            ?.toSet()
            .orEmpty()
        val wbPresets = WhiteBalancePreset.entries.filter { it.camera2Mode in awbModes }
            .ifEmpty { listOf(WhiteBalancePreset.AUTO) }

        val fpsList = querySupportedFpsForCameraId(c2Info.cameraId)
        val stabSupported = querySupportsVideoStabilization(c2Info.cameraId)

        return CameraHardwareCapabilities(
            availableLenses = allLenses,
            activeLens = activeLens,
            availableApertures = apertures,
            minZoomRatio = zoom?.minZoomRatio ?: 1f,
            maxZoomRatio = zoom?.maxZoomRatio ?: 1f,
            exposureMinIndex = if (exp.isExposureCompensationSupported) exp.exposureCompensationRange.lower else 0,
            exposureMaxIndex = if (exp.isExposureCompensationSupported) exp.exposureCompensationRange.upper else 0,
            exposureStepEv = if (exp.isExposureCompensationSupported) exp.exposureCompensationStep.toFloat() else 0.33f,
            supportsManualExposure = supportsManual,
            isoMin = isoRange?.lower ?: 100,
            isoMax = (isoRange?.upper ?: 1600).coerceAtMost(6400),
            shutterMinNs = shutterRange?.lower ?: 1_000_000L,
            shutterMaxNs = (shutterRange?.upper ?: 33_333_333L).coerceAtMost(100_000_000L),
            supportedWhiteBalance = wbPresets,
            supportedQualities = supportedQualities,
            supportedFps = fpsList,
            supportsVideoStabilization = stabSupported,
            hasFlashUnit = info.hasFlashUnit(),
        )
    }

    private fun querySupportedFpsForCameraId(cameraId: String?): List<Int> {
        if (cameraId == null) return listOf(30)
        return try {
            val manager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val ranges = manager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                .orEmpty()
            val fps = mutableListOf(30)
            if (ranges.any { it.upper >= 60 }) {
                fps.add(60)
            }
            fps
        } catch (_: Exception) {
            listOf(30)
        }
    }

    private fun querySupportsVideoStabilization(cameraId: String?): Boolean {
        if (cameraId == null) return false
        return try {
            val manager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val modes = manager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                ?: intArrayOf()
            modes.any { it != CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Queries Android CamcorderProfile / EncoderProfiles directly via CameraManager in priority order
     * without invoking CameraX CapabilitiesByQuality when profiles are absent.
     */
    private fun queryEncoderQualitiesForCameraId(cameraId: String?): List<Quality> {
        if (cameraId == null) return emptyList()
        val candidates = listOf(
            CamcorderProfile.QUALITY_1080P to Quality.FHD,
            CamcorderProfile.QUALITY_720P to Quality.HD,
            CamcorderProfile.QUALITY_480P to Quality.SD,
            CamcorderProfile.QUALITY_2160P to Quality.UHD,
            CamcorderProfile.QUALITY_HIGH to Quality.HIGHEST,
            CamcorderProfile.QUALITY_LOW to Quality.LOWEST,
        )
        val supported = mutableListOf<Quality>()
        for ((profileId, quality) in candidates) {
            if (hasEncoderProfile(cameraId, profileId) && quality !in supported) {
                supported.add(quality)
            }
        }
        return supported
    }

    private fun findCameraIdForFacing(isFront: Boolean): String? {
        val targetFacing = if (isFront) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        return try {
            val manager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            manager.cameraIdList.firstOrNull { id ->
                val facing = manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING)
                facing == targetFacing
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao inspecionar CameraManager: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun hasEncoderProfile(cameraId: String, profileQuality: Int): Boolean {
        val numericId = cameraId.toIntOrNull() ?: return false
        val exists = try {
            @Suppress("DEPRECATION")
            CamcorderProfile.hasProfile(numericId, profileQuality)
        } catch (_: Exception) {
            false
        }
        if (!exists) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return try {
                val all = CamcorderProfile.getAll(cameraId, profileQuality)
                all != null && all.videoProfiles.any { it != null && it.width > 0 && it.height > 0 }
            } catch (_: Exception) {
                true
            }
        }
        return true
    }

    private fun isAndroidEmulator(): Boolean {
        val fp = Build.FINGERPRINT.lowercase(Locale.ROOT)
        val model = Build.MODEL.lowercase(Locale.ROOT)
        val product = Build.PRODUCT.lowercase(Locale.ROOT)
        val hardware = Build.HARDWARE.lowercase(Locale.ROOT)
        val board = Build.BOARD.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)
        val device = Build.DEVICE.lowercase(Locale.ROOT)
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        return fp.startsWith("generic") ||
            fp.startsWith("unknown") ||
            fp.contains("emulator") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            hardware.contains("cutf") ||
            board.contains("goldfish") ||
            board.contains("ranchu") ||
            board.contains("cutf") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            model.contains("cuttlefish") ||
            product.contains("sdk_gphone") ||
            product.contains("google_sdk") ||
            product.contains("emulator") ||
            product.contains("simulator") ||
            product.contains("vsoc") ||
            product.contains("aosp_cf") ||
            product.startsWith("cf_") ||
            device.contains("vsoc") ||
            device.startsWith("emu64") ||
            manufacturer.contains("genymotion") ||
            (brand.startsWith("generic") && device.startsWith("generic"))
    }

    fun startRecording(
        isShortTest: Boolean,
        onStarted: () -> Unit,
        onStatus: (elapsedMillis: Long, audioAmplitude: Double, hasAudio: Boolean) -> Unit,
        onFinalizedSuccess: (uri: Uri, displayName: String, durationMillis: Long, qualityLabel: String) -> Unit,
        onFinalizedError: (message: String) -> Unit,
    ): Boolean {
        val capture = videoCapture ?: run {
            onFinalizedError("A gravação de vídeo não está disponível nesta câmera/emulador.")
            return false
        }

        if (activeRecording != null) {
            return false
        }

        val hasAudioPerm = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasAudioPerm) {
            onFinalizedError("Permissão de microfone necessária para gravar o áudio do vídeo.")
            return false
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val prefix = if (isShortTest) "Teste_EscutaEGrava_" else "EscutaEGrava_"
        val displayName = "$prefix$timestamp.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/EscutaEGrava")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            appContext.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
            .setContentValues(contentValues)
            .build()

        return try {
            val pending = capture.output
                .prepareRecording(appContext, outputOptions)
                .withAudioEnabled()

            activeRecording = pending.start(ContextCompat.getMainExecutor(appContext)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        Log.i(TAG, "Gravação iniciada com confirmação do CameraX")
                        onStarted()
                    }

                    is VideoRecordEvent.Status -> {
                        val stats = event.recordingStats
                        val elapsedMs = stats.recordedDurationNanos / 1_000_000L
                        val audioStats = stats.audioStats
                        val amp = if (audioStats.hasAudio()) audioStats.audioAmplitude else 0.0
                        onStatus(elapsedMs, amp, audioStats.hasAudio())
                    }

                    is VideoRecordEvent.Finalize -> {
                        activeRecording = null
                        val durationMs = event.recordingStats.recordedDurationNanos / 1_000_000L
                        val uri = event.outputResults.outputUri
                        if (!event.hasError() && uri != Uri.EMPTY) {
                            Log.i(TAG, "Vídeo salvo com sucesso (duração=${durationMs}ms)")
                            onFinalizedSuccess(uri, displayName, durationMs, lastEffectiveQualityLabel)
                        } else {
                            val errMsg = mapFinalizeError(event.error)
                            Log.e(TAG, "Finalização da gravação falhou (code=${event.error})")
                            onFinalizedError(errMsg)
                        }
                    }
                }
            }
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permissão negada ao iniciar gravação")
            onFinalizedError("Permissão de câmera ou microfone recusada pelo sistema.")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado ao iniciar gravação: ${e.javaClass.simpleName}")
            onFinalizedError("Não foi possível iniciar a gravação neste momento.")
            false
        }
    }

    fun stopRecording() {
        try {
            activeRecording?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao solicitar parada da gravação: ${e.javaClass.simpleName}")
        }
    }

    fun release() {
        stopRecording()
        activeRecording = null
        boundCamera = null
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }
        videoCapture = null
    }

    private fun qualityToLabel(quality: Quality): String = when (quality) {
        Quality.UHD -> "4K (2160p)"
        Quality.FHD -> "1080p"
        Quality.HD -> "720p"
        Quality.SD -> "480p"
        Quality.HIGHEST -> "Alta"
        Quality.LOWEST -> "Básica"
        else -> "Padrão"
    }

    private fun formatAperture(value: Float): String {
        return String.format(Locale.US, "f/%.1f", value)
    }

    private fun mapFinalizeError(errorCode: Int): String = when (errorCode) {
        VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE ->
            "Armazenamento insuficiente no aparelho. O vídeo não foi salvo."
        VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE ->
            "A câmera foi interrompida antes de concluir o vídeo."
        VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA ->
            "Nenhum dado válido de vídeo foi gravado. Tente gravar por mais de 1 segundo."
        VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED ->
            "Limite máximo de tamanho de arquivo atingido."
        VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED ->
            "Limite máximo de duração atingido."
        VideoRecordEvent.Finalize.ERROR_INVALID_OUTPUT_OPTIONS ->
            "Não foi possível criar o arquivo de vídeo na galeria."
        VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED,
        VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR ->
            "Erro ao codificar ou finalizar o arquivo de vídeo."
        else ->
            "A gravação foi encerrada com erro (código $errorCode) e o arquivo pode não ter sido salvo."
    }

    companion object {
        private const val TAG = "CameraRecorderCtrl"
    }
}
