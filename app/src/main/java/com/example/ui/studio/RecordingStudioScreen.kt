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
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideocamOff
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.camera.CameraBindResult
import com.example.camera.CameraRecorderController
import com.example.model.AudioRouteInfo
import com.example.model.AudioRouteKind
import com.example.model.PlaybackState
import com.example.model.RecordingState
import com.example.model.TeleprompterPreferences
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RecCrimson
import com.example.ui.theme.RouteAmber
import com.example.ui.theme.RouteEmerald
import com.example.ui.theme.StudioDarkCanvas
import com.example.viewmodel.StudioUiState

@Composable
fun RecordingStudioScreen(
    scriptTitle: String,
    sentences: List<String>,
    preferences: TeleprompterPreferences,
    studioState: StudioUiState,
    playbackState: PlaybackState,
    recordingState: RecordingState,
    audioRoute: AudioRouteInfo,
    cameraController: CameraRecorderController,
    onCameraBound: (CameraBindResult) -> Unit,
    onCameraError: (String) -> Unit,
    onToggleCameraLens: () -> Unit,
    onTogglePanelExpanded: () -> Unit,
    onCyclePanelHeight: () -> Unit,
    onAdjustFontSize: (Float) -> Unit,
    onPhraseTapped: (Int) -> Unit,
    onPlayPauseReading: () -> Unit,
    onRepeatPhrase: () -> Unit,
    onPreviousPhrase: () -> Unit,
    onNextPhrase: () -> Unit,
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
            // Layer 1: Real CameraX PreviewView (UI overlays below are never recorded into the video)
            val previewView = remember {
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }

            LaunchedEffect(studioState.isFrontCamera, permissionsGranted) {
                if (permissionsGranted) {
                    cameraController.bindCamera(
                        lifecycleOwner = lifecycleOwner,
                        previewView = previewView,
                        useFrontCamera = studioState.isFrontCamera,
                        onBound = onCameraBound,
                        onError = onCameraError,
                    )
                }
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )

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
                    onBack = requestExit,
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

            // Layer 5: Bottom independent Reading + Recording dock
            StudioControls(
                playbackState = playbackState,
                recordingState = recordingState,
                cameraReady = studioState.cameraReady,
                hasSentences = sentences.isNotEmpty(),
                onPreviousPhrase = onPreviousPhrase,
                onRepeatPhrase = onRepeatPhrase,
                onPlayPauseReading = onPlayPauseReading,
                onNextPhrase = onNextPhrase,
                onStartRecording = { onStartRecording(false) },
                onStartShortTest = { onStartRecording(true) },
                onStopRecording = onStopRecording,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
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
    onBack: () -> Unit,
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        // Center status pill: REC timer OR Camera quality + Lens badge
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
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = recBadgeModifier,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        val lensLabel = if (studioState.isFrontCamera) "Frontal" else "Traseira"
                        Text(
                            text = "$lensLabel • ${studioState.effectiveQualityLabel}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // Compact audio route status icon pill
        val routeColor = if (audioRoute.isHeadsetSafe) RouteEmerald else RouteAmber
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
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
                    contentDescription = audioRoute.description,
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
