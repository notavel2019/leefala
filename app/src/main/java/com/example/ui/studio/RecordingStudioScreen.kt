package com.example.ui.studio

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.camera.CameraBindResult
import com.example.camera.CameraRecorderController
import com.example.model.AudioRouteInfo
import com.example.model.AudioRouteKind
import com.example.model.CameraHardwareCapabilities
import com.example.model.CameraSettingsState
import com.example.model.PlaybackState
import com.example.model.RecordingState
import com.example.model.TeleprompterPreferences
import com.example.model.WhiteBalancePreset
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RecCrimson
import com.example.ui.theme.RouteAmber
import com.example.ui.theme.RouteEmerald
import com.example.ui.theme.StudioDarkCanvas
import com.example.ui.theme.StudioTealOnDark
import com.example.viewmodel.StudioUiState
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun RecordingStudioScreen(
    scriptTitle: String,
    sentences: List<String>,
    preferences: TeleprompterPreferences,
    studioState: StudioUiState,
    playbackState: PlaybackState,
    recordingState: RecordingState,
    audioRoute: AudioRouteInfo,
    cameraCapabilities: CameraHardwareCapabilities,
    cameraSettings: CameraSettingsState,
    cameraRebindVersion: Int,
    cameraController: CameraRecorderController,
    onCameraBound: (CameraBindResult) -> Unit,
    onCameraError: (String) -> Unit,
    onToggleCameraLens: () -> Unit,
    onSelectCameraLens: (String, Boolean) -> Unit,
    onSelectAperture: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onExposureIndexChange: (Int) -> Unit,
    onManualExposureToggle: (Boolean) -> Unit,
    onManualIsoChange: (Int) -> Unit,
    onManualShutterChange: (Long) -> Unit,
    onSelectWhiteBalance: (WhiteBalancePreset) -> Unit,
    onSelectQuality: (String) -> Unit,
    onSelectFps: (Int) -> Unit,
    onVideoStabilizationToggle: (Boolean) -> Unit,
    onTorchToggle: (Boolean) -> Unit,
    onAeLockToggle: (Boolean) -> Unit,
    onResetCameraControls: () -> Unit,
    onTogglePanelExpanded: () -> Unit,
    onCyclePanelHeight: () -> Unit,
    onAdjustFontSize: (Float) -> Unit,
    onPhraseTapped: (Int) -> Unit,
    onPlayPauseReading: () -> Unit,
    onRepeatPhrase: () -> Unit,
    onPreviousPhrase: () -> Unit,
    onNextPhrase: () -> Unit,
    onReturnVolumeChange: (Float) -> Unit,
    onStepReturnVolume: (Float) -> Unit,
    isProUnlocked: Boolean,
    onOpenProUpgrade: () -> Unit,
    onStartRecording: (Boolean) -> Unit,
    onStopRecording: () -> Unit,
    onDismissAlerts: () -> Unit,
    onDismissRecordingResult: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onShareVideo: (String) -> Unit,
    onOpenAppSettings: () -> Unit,
    onExitStudio: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep screen awake during the entire studio session
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    var permissionsGranted by remember { mutableStateOf(hasAllPermissions(context, requiredPermissions)) }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionRequestedOnce = true
        permissionsGranted = requiredPermissions.all { perm ->
            result[perm] == true || ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Re-check permissions when returning from Settings without creating a request loop
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                permissionsGranted = hasAllPermissions(context, requiredPermissions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showExitRecordingDialog by remember { mutableStateOf(false) }
    var showCameraSettingsSheet by remember { mutableStateOf(false) }
    var showReturnVolumeBar by remember { mutableStateOf(false) }
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            delay(1200L)
            focusTapOffset = null
        }
    }

    val requestExit: () -> Unit = {
        if (recordingState.isSessionLocked) {
            showExitRecordingDialog = true
        } else {
            onExitStudio()
        }
    }

    BackHandler(enabled = true) {
        requestExit()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioDarkCanvas),
    ) {
        if (!permissionsGranted) {
            PermissionsExplanationView(
                permissionRequestedOnce = permissionRequestedOnce,
                onRequestPermissions = { permissionLauncher.launch(requiredPermissions) },
                onOpenAppSettings = onOpenAppSettings,
                onBack = onExitStudio,
            )
        } else {
            // Layer 1: Real CameraX PreviewView + Tap-to-focus (UI overlays below are never recorded into the video)
            val previewView = remember {
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }

            LaunchedEffect(cameraRebindVersion, permissionsGranted) {
                if (permissionsGranted) {
                    cameraController.bindCamera(
                        lifecycleOwner = lifecycleOwner,
                        previewView = previewView,
                        useFrontCamera = studioState.isFrontCamera,
                        settings = cameraSettings,
                        onBound = onCameraBound,
                        onError = onCameraError,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            focusTapOffset = tapOffset
                            cameraController.tapToFocus(previewView, tapOffset.x, tapOffset.y)
                        }
                    },
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize(),
                )

                focusTapOffset?.let { tap ->
                    Surface(
                        color = Color.Transparent,
                        shape = CircleShape,
                        border = BorderStroke(2.dp, StudioTealOnDark),
                        modifier = Modifier
                            .offset { IntOffset((tap.x - 32.dp.toPx()).roundToInt(), (tap.y - 32.dp.toPx()).roundToInt()) }
                            .size(64.dp),
                    ) {}
                }
            }

            // Subtle top & bottom gradient scrims for crisp overlay legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
            )

            // Layer 2: Top Bar + Alerts + Teleprompter Panel (kept near top camera lens for natural eye line)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StudioTopBar(
                    studioState = studioState,
                    recordingState = recordingState,
                    audioRoute = audioRoute,
                    activeAperture = cameraSettings.selectedAperture ?: cameraCapabilities.availableApertures.firstOrNull(),
                    isVolumeBarOpen = showReturnVolumeBar,
                    onBack = requestExit,
                    onOpenCameraSettings = { showCameraSettingsSheet = true },
                    onToggleVolumeBar = { showReturnVolumeBar = !showReturnVolumeBar },
                    onToggleCameraLens = onToggleCameraLens,
                )

                // Warnings: Bluetooth disconnect / TTS error / Camera error / Interruption
                val activeWarning = studioState.headsetDisconnectedWarning
                    ?: studioState.cameraError
                    ?: studioState.interruptionMessage
                    ?: playbackState.errorMessage

                AnimatedVisibility(visible = activeWarning != null) {
                    activeWarning?.let { warningText ->
                        Surface(
                            color = Color(0xEE7C2D12),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_warning_banner"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = warningText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = onDismissAlerts,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Fechar aviso",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Collapsible & resizable semi-transparent script overlay
                TeleprompterOverlay(
                    scriptTitle = scriptTitle,
                    sentences = sentences,
                    playbackState = playbackState,
                    isExpanded = studioState.isScriptPanelExpanded,
                    panelHeightMode = preferences.panelHeightMode,
                    fontSizeSp = preferences.overlayFontSizeSp,
                    onToggleExpanded = onTogglePanelExpanded,
                    onCycleHeight = onCyclePanelHeight,
                    onFontSmaller = { onAdjustFontSize(-2f) },
                    onFontLarger = { onAdjustFontSize(2f) },
                    onPhraseTapped = onPhraseTapped,
                )
            }

            // Layer 3: Centered 3-second visual countdown (ONLY after CameraX VideoRecordEvent.Start)
            if (recordingState is RecordingState.Countdown) {
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.68f),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("countdown_overlay"),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 36.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "GRAVANDO • LEITURA EM",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFCA5A5),
                                letterSpacing = 1.2.sp,
                            )
                            Text(
                                text = "${recordingState.secondsRemaining}",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 68.sp,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            // Layer 4: Finalizing spinner indicator
            if (recordingState is RecordingState.Finalizing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = Color(0xFF131C24),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Text(
                                text = "Finalizando e salvando vídeo...",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                            )
                            Text(
                                text = "Aguarde a confirmação antes de sair.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                            )
                        }
                    }
                }
            }

            // Layer 5: Live Return Volume Bar + Quick Zoom/Volume Strip + Bottom Reading & Recording dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedVisibility(visible = showReturnVolumeBar) {
                    StudioReturnVolumeBar(
                        volume = preferences.returnVolume,
                        audioRoute = audioRoute,
                        onVolumeChange = onReturnVolumeChange,
                        onStepVolume = onStepReturnVolume,
                        onClose = { showReturnVolumeBar = false },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }

                val zoomPresets = if (cameraCapabilities.maxZoomRatio > cameraCapabilities.minZoomRatio + 0.05f) {
                    listOf(0.5f, 0.6f, 1.0f, 2.0f, 3.0f).filter {
                        it >= cameraCapabilities.minZoomRatio - 0.02f &&
                            it <= cameraCapabilities.maxZoomRatio + 0.02f
                    }
                } else {
                    emptyList()
                }
                val volPercent = (preferences.returnVolume * 100f).roundToInt().coerceIn(0, 100)

                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (zoomPresets.size >= 2) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.48f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                zoomPresets.forEach { preset ->
                                    val isSelected = abs(cameraSettings.zoomRatio - preset) < 0.08f
                                    Surface(
                                        onClick = { onZoomChange(preset) },
                                        color = if (isSelected) StudioTealOnDark else Color.Transparent,
                                        shape = RoundedCornerShape(999.dp),
                                    ) {
                                        Text(
                                            text = if (preset == preset.roundToInt().toFloat()) {
                                                "${preset.roundToInt()}x"
                                            } else {
                                                String.format(Locale("pt", "BR"), "%.1fx", preset)
                                            },
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF0B1116) else Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Always-accessible Return Audio Volume toggle pill (works even while recording)
                    Surface(
                        onClick = { showReturnVolumeBar = !showReturnVolumeBar },
                        color = if (showReturnVolumeBar) StudioTealOnDark else Color.Black.copy(alpha = 0.56f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(
                            1.dp,
                            if (showReturnVolumeBar) StudioTealOnDark else Color.White.copy(alpha = 0.2f),
                        ),
                        modifier = Modifier.testTag("studio_volume_pill"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = when {
                                    volPercent == 0 -> Icons.Filled.VolumeOff
                                    volPercent < 45 -> Icons.Filled.VolumeDown
                                    else -> Icons.Filled.VolumeUp
                                },
                                contentDescription = "Volume do retorno",
                                tint = if (showReturnVolumeBar) Color(0xFF0B1116) else Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (volPercent == 0) "Mudo" else "$volPercent%",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showReturnVolumeBar) Color(0xFF0B1116) else Color.White,
                            )
                        }
                    }
                    if (!isProUnlocked && !recordingState.isSessionLocked) {
                        Surface(
                            onClick = onOpenProUpgrade,
                            color = StudioTealOnDark.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, StudioTealOnDark.copy(alpha = 0.65f)),
                            modifier = Modifier.testTag("studio_trial_mode_pill"),
                        ) {
                            Text(
                                text = "Modo Teste • Pro",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTealOnDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            )
                        }
                    }
                }

                StudioControls(
                    playbackState = playbackState,
                    recordingState = recordingState,
                    cameraReady = studioState.cameraReady,
                    hasSentences = sentences.isNotEmpty(),
                    isProUnlocked = isProUnlocked,
                    onPreviousPhrase = onPreviousPhrase,
                    onRepeatPhrase = onRepeatPhrase,
                    onPlayPauseReading = onPlayPauseReading,
                    onNextPhrase = onNextPhrase,
                    onStartRecording = { onStartRecording(false) },
                    onStartShortTest = { onStartRecording(true) },
                    onStopRecording = onStopRecording,
                )
            }
        }
    }

    if (showCameraSettingsSheet) {
        CameraSettingsSheet(
            capabilities = cameraCapabilities,
            settings = cameraSettings,
            effectiveQualityLabel = studioState.effectiveQualityLabel,
            isRecordingLocked = recordingState.isSessionLocked,
            onSelectLens = onSelectCameraLens,
            onSelectAperture = onSelectAperture,
            onZoomChange = onZoomChange,
            onExposureIndexChange = onExposureIndexChange,
            onManualExposureToggle = onManualExposureToggle,
            onManualIsoChange = onManualIsoChange,
            onManualShutterChange = onManualShutterChange,
            onSelectWhiteBalance = onSelectWhiteBalance,
            onSelectQuality = onSelectQuality,
            onSelectFps = onSelectFps,
            onVideoStabilizationToggle = onVideoStabilizationToggle,
            onTorchToggle = onTorchToggle,
            onAeLockToggle = onAeLockToggle,
            onResetToAuto = onResetCameraControls,
            onDismiss = { showCameraSettingsSheet = false },
        )
    }

    // Confirmed Video Saved Sheet (shown ONLY on VideoRecordEvent.Finalize with no error)
    if (recordingState is RecordingState.Saved) {
        VideoResultSheet(
            saved = recordingState,
            onOpenVideo = { onOpenVideo(recordingState.uriString) },
            onShareVideo = { onShareVideo(recordingState.uriString) },
            onDismiss = onDismissRecordingResult,
        )
    }

    // Recording Error Dialog (honest state reporting)
    if (recordingState is RecordingState.Error) {
        AlertDialog(
            onDismissRequest = onDismissRecordingResult,
            title = { Text("Gravação não concluída") },
            text = { Text(recordingState.message) },
            confirmButton = {
                Button(onClick = onDismissRecordingResult) {
                    Text("Entendi")
                }
            },
        )
    }

    // Confirmation before voluntary exit while recording
    if (showExitRecordingDialog) {
        AlertDialog(
            onDismissRequest = { showExitRecordingDialog = false },
            title = { Text("Encerrar gravação?") },
            text = {
                Text("Há uma gravação em andamento. Se você sair agora, a gravação será encerrada e a leitura cancelada.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = RecCrimson),
                    onClick = {
                        showExitRecordingDialog = false
                        onStopRecording()
                    },
                ) {
                    Text("Finalizar vídeo aqui")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showExitRecordingDialog = false }) {
                        Text("Continuar gravando")
                    }
                    TextButton(
                        onClick = {
                            showExitRecordingDialog = false
                            onExitStudio()
                        },
                    ) {
                        Text("Sair", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
    }
}

@Composable
private fun StudioTopBar(
    studioState: StudioUiState,
    recordingState: RecordingState,
    audioRoute: AudioRouteInfo,
    activeAperture: Float?,
    isVolumeBarOpen: Boolean,
    onBack: () -> Unit,
    onOpenCameraSettings: () -> Unit,
    onToggleVolumeBar: () -> Unit,
    onToggleCameraLens: () -> Unit,
) {
    val isLocked = recordingState.isSessionLocked
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "rec_alpha",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Back button
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = CircleShape,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("studio_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Color.White,
                )
            }
        }

        // Center status pill: REC timer OR Camera quality + Lens + Aperture badge
        val recBadgeModifier = Modifier.weight(1f)
        when (recordingState) {
            is RecordingState.Recording,
            is RecordingState.Countdown,
            is RecordingState.Starting -> {
                val elapsed = (recordingState as? RecordingState.Recording)?.elapsedMillis ?: 0L
                val isShort = when (recordingState) {
                    is RecordingState.Recording -> recordingState.isShortTest
                    is RecordingState.Countdown -> recordingState.isShortTest
                    is RecordingState.Starting -> recordingState.isShortTest
                    else -> false
                }
                Surface(
                    color = RecCrimson.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = recBadgeModifier.testTag("rec_status_pill"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(Color.White),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isShort) "TESTE • ${formatElapsed(elapsed)}" else "REC • ${formatElapsed(elapsed)}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                        )
                    }
                }
            }

            else -> {
                Surface(
                    onClick = onOpenCameraSettings,
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = recBadgeModifier,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        val lensLabel = if (studioState.isFrontCamera) "Frontal" else "Traseira"
                        val apertureSuffix = activeAperture?.let { String.format(Locale.US, " • f/%.1f", it) } ?: ""
                        Text(
                            text = "$lensLabel$apertureSuffix • ${studioState.effectiveQualityLabel}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Pro Camera Settings button (lenses, aperture, EV, ISO, WB, FPS, stabilization)
        Surface(
            color = Color.Black.copy(alpha = 0.58f),
            shape = CircleShape,
        ) {
            IconButton(
                onClick = onOpenCameraSettings,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("camera_settings_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Configurações da câmera",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        // Compact audio route status icon pill (tapping opens live return volume control)
        val routeColor = if (audioRoute.isHeadsetSafe) RouteEmerald else RouteAmber
        Surface(
            onClick = onToggleVolumeBar,
            color = if (isVolumeBarOpen) routeColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.6f),
            shape = CircleShape,
            border = BorderStroke(1.5.dp, routeColor),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .testTag("studio_audio_route_badge"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (audioRoute.kind) {
                        AudioRouteKind.BLUETOOTH_MEDIA,
                        AudioRouteKind.BLUETOOTH_CALL_SCO -> Icons.Filled.BluetoothAudio
                        AudioRouteKind.WIRED_HEADSET -> Icons.Filled.Headphones
                        else -> Icons.Filled.VolumeUp
                    },
                    contentDescription = "${audioRoute.description} — ajustar volume do retorno",
                    tint = if (audioRoute.isHeadsetSafe) Color(0xFF6EE7B7) else Color(0xFFFCD34D),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Flip camera button (strictly disabled while recording)
        if (studioState.hasOtherCamera) {
            Surface(
                color = Color.Black.copy(alpha = if (isLocked) 0.25f else 0.55f),
                shape = CircleShape,
            ) {
                IconButton(
                    onClick = onToggleCameraLens,
                    enabled = !isLocked,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("switch_camera_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = stringResource(R.string.cd_switch_camera),
                        tint = if (isLocked) Color.White.copy(alpha = 0.35f) else Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioReturnVolumeBar(
    volume: Float,
    audioRoute: AudioRouteInfo,
    onVolumeChange: (Float) -> Unit,
    onStepVolume: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val percent = (volume * 100f).roundToInt().coerceIn(0, 100)
    Surface(
        color = Color(0xE00F172A),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, StudioTealOnDark.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_return_volume_bar"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Volume do retorno • ${if (percent == 0) "Mudo" else "$percent%"}",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTealOnDark,
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fechar controle de volume",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    onClick = { onVolumeChange(if (volume > 0.01f) 0f else 0.70f) },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("studio_volume_mute_button"),
                ) {
                    Icon(
                        imageVector = when {
                            percent == 0 -> Icons.Filled.VolumeOff
                            percent < 45 -> Icons.Filled.VolumeDown
                            else -> Icons.Filled.VolumeUp
                        },
                        contentDescription = "Silenciar retorno",
                        tint = Color.White,
                    )
                }

                IconButton(
                    onClick = { onStepVolume(-0.10f) },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("studio_volume_down_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Diminuir volume do retorno",
                        tint = Color.White,
                    )
                }

                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioTealOnDark,
                        activeTrackColor = StudioTealOnDark,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("studio_volume_slider"),
                )

                IconButton(
                    onClick = { onStepVolume(0.10f) },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("studio_volume_up_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Aumentar volume do retorno",
                        tint = Color.White,
                    )
                }
            }

            Text(
                text = audioRoute.description,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PermissionsExplanationView(
    permissionRequestedOnce: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = Color(0xFF131C24),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF263544)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF134E4A)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideocamOff,
                        contentDescription = null,
                        tint = Color(0xFF5EEAD4),
                        modifier = Modifier.size(28.dp),
                    )
                }

                Text(
                    text = "Permissões para gravar",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "O Escuta e Grava precisa de acesso à Câmera e ao Microfone para exibir a prévia e gravar sua fala enquanto você ouve o roteiro no fone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                )

                if (permissionRequestedOnce) {
                    Text(
                        text = "Se você negou alguma permissão, libere Câmera e Microfone nas configurações do aplicativo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCD34D),
                        textAlign = TextAlign.Center,
                    )
                }

                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_permissions_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Permitir câmera e microfone")
                }

                if (permissionRequestedOnce) {
                    OutlinedButton(
                        onClick = onOpenAppSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_app_settings_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir configurações do app")
                    }
                }

                TextButton(onClick = onBack) {
                    Text("Voltar para o roteiro", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

private fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean {
    return permissions.all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}

fun formatElapsed(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format("%02d:%02d", minutes, seconds)
}
