package com.example.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.CamcorderProfile
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CameraBindResult(
    val effectiveQualityLabel: String,
    val isFrontCamera: Boolean,
    val hasOtherCamera: Boolean,
    val videoCaptureSupported: Boolean = true,
    val warningMessage: String? = null,
)

/**
 * Real CameraX 1.5.0 video capture controller.
 * - Portrait vertical output with MirrorMode.MIRROR_MODE_OFF (keeps physical scene text readable).
 * - Pre-checks Android CamcorderProfile / EncoderProfiles via CameraManager before instantiating
 *   CameraX Recorder/VideoCapture so virtual/emulator cameras without encoder profiles gracefully
 *   bind Preview-only without triggering CapabilitiesByQuality errors.
 * - Requests 1080p (FHD) with real fallback to 720p (HD) or 480p (SD), reporting actual quality.
 * - Saves directly to MediaStore Movies/EscutaEGrava and only confirms success on Finalize with no error.
 */
class CameraRecorderController(context: Context) {

    private val appContext = context.applicationContext
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var lastEffectiveQualityLabel: String = "1080p"

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean,
        onBound: (CameraBindResult) -> Unit,
        onError: (String) -> Unit,
    ) {
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener(
            {
                try {
                    val provider = future.get()
                    cameraProvider = provider

                    val hasFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                    val hasBack = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)

                    var actualIsFront = when {
                        useFrontCamera && hasFront -> true
                        !useFrontCamera && hasBack -> false
                        hasFront -> true
                        hasBack -> false
                        else -> {
                            onError("Nenhuma câmera disponível neste aparelho.")
                            return@addListener
                        }
                    }

                    // Pre-check real EncoderProfiles via CameraManager/CamcorderProfile before touching Recorder
                    var supportedQualities = queryEncoderQualitiesForLens(isFront = actualIsFront)

                    // If requested lens has no encoder profiles but the other lens does, switch cleanly
                    if (supportedQualities.isEmpty() && hasFront && hasBack) {
                        val altQualities = queryEncoderQualitiesForLens(isFront = !actualIsFront)
                        if (altQualities.isNotEmpty()) {
                            actualIsFront = !actualIsFront
                            supportedQualities = altQualities
                        }
                    }

                    val selector = if (actualIsFront) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val preview = Preview.Builder()
                        .setTargetRotation(Surface.ROTATION_0)
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }

                    provider.unbindAll()

                    if (supportedQualities.isEmpty()) {
                        // Bind Preview-only cleanly when the device/emulator exposes no video EncoderProfiles
                        provider.bindToLifecycle(lifecycleOwner, selector, preview)
                        videoCapture = null
                        lastEffectiveQualityLabel = "Somente prévia"
                        onBound(
                            CameraBindResult(
                                effectiveQualityLabel = "Somente prévia",
                                isFrontCamera = actualIsFront,
                                hasOtherCamera = hasFront && hasBack,
                                videoCaptureSupported = false,
                                warningMessage = "Prévia da câmera ativa, mas este emulador não disponibiliza perfis de gravação de vídeo. Instale o APK em um celular Android físico para gravar.",
                            )
                        )
                        return@addListener
                    }

                    val resolvedQuality = supportedQualities.first()
                    lastEffectiveQualityLabel = qualityToLabel(resolvedQuality)

                    val qualitySelector = QualitySelector.fromOrderedList(
                        supportedQualities,
                        FallbackStrategy.lowerQualityOrHigherThan(resolvedQuality),
                    )

                    val recorder = Recorder.Builder()
                        .setQualitySelector(qualitySelector)
                        .build()

                    val vc = VideoCapture.Builder(recorder)
                        .setDynamicRange(DynamicRange.SDR)
                        .setMirrorMode(MirrorMode.MIRROR_MODE_OFF)
                        .setTargetRotation(Surface.ROTATION_0)
                        .build()

                    try {
                        provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
                        videoCapture = vc
                        onBound(
                            CameraBindResult(
                                effectiveQualityLabel = lastEffectiveQualityLabel,
                                isFrontCamera = actualIsFront,
                                hasOtherCamera = hasFront && hasBack,
                                videoCaptureSupported = true,
                            )
                        )
                    } catch (bindEx: Exception) {
                        Log.w(TAG, "VideoCapture indisponível nesta câmera (${bindEx.javaClass.simpleName}); mantendo prévia")
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, preview)
                        videoCapture = null
                        lastEffectiveQualityLabel = "Somente prévia"
                        onBound(
                            CameraBindResult(
                                effectiveQualityLabel = "Somente prévia",
                                isFrontCamera = actualIsFront,
                                hasOtherCamera = hasFront && hasBack,
                                videoCaptureSupported = false,
                                warningMessage = "Prévia ativa, mas não foi possível vincular a gravação de vídeo nesta câmera.",
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
     * Queries Android CamcorderProfile / EncoderProfiles directly via CameraManager in priority order
     * (1080p -> 720p -> 480p -> 2160p) without invoking CameraX CapabilitiesByQuality when profiles are absent.
     */
    private fun queryEncoderQualitiesForLens(isFront: Boolean): List<Quality> {
        val cameraId = findCameraIdForFacing(isFront) ?: return emptyList()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val all = CamcorderProfile.getAll(cameraId, profileQuality)
                if (all != null && all.videoProfiles.any { it != null && it.width > 0 && it.height > 0 }) {
                    return true
                }
            } catch (_: Exception) {
            }
        }
        val numericId = cameraId.toIntOrNull()
        if (numericId != null) {
            try {
                @Suppress("DEPRECATION")
                if (CamcorderProfile.hasProfile(numericId, profileQuality)) {
                    return true
                }
            } catch (_: Exception) {
            }
        }
        return false
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
        else -> "Qualidade padrão"
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
