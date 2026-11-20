package com.example.ui.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PlaybackMode
import com.example.model.PlaybackState
import com.example.model.RecordingState
import com.example.ui.theme.RecCrimson
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealOnDark

@Composable
fun StudioControls(
    playbackState: PlaybackState,
    recordingState: RecordingState,
    cameraReady: Boolean,
    hasSentences: Boolean,
    onPreviousPhrase: () -> Unit,
    onRepeatPhrase: () -> Unit,
    onPlayPauseReading: () -> Unit,
    onNextPhrase: () -> Unit,
    onStartRecording: () -> Unit,
    onStartShortTest: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReadingActive = playbackState.isActiveContinuous || playbackState.mode == PlaybackMode.SPEAKING_SINGLE
    val isRecordingActive = recordingState is RecordingState.Recording ||
        recordingState is RecordingState.Countdown ||
        recordingState is RecordingState.Starting
    val isFinalizing = recordingState is RecordingState.Finalizing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Row 1: Independent Reading Controls (NEVER pause/stop/restart video recording)
        Surface(
            color = Color(0xCC0F172A),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onPreviousPhrase,
                    enabled = hasSentences && !isFinalizing,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("previous_phrase_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_previous_phrase),
                        tint = Color.White,
                    )
                }

                IconButton(
                    onClick = onRepeatPhrase,
                    enabled = hasSentences && !isFinalizing,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("repeat_phrase_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = stringResource(R.string.cd_repeat_phrase),
                        tint = StudioTealOnDark,
                    )
                }

                Button(
                    onClick = onPlayPauseReading,
                    enabled = hasSentences && !isFinalizing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReadingActive) Color(0xFF334155) else StudioTeal,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("play_pause_reading_button"),
                ) {
                    Icon(
                        imageVector = if (isReadingActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause_reading),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            isReadingActive -> "Pausar leitura"
                            playbackState.mode == PlaybackMode.PAUSED -> "Retomar"
                            !isRecordingActive -> "Ensaiar"
                            else -> "Ler"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                IconButton(
                    onClick = onNextPhrase,
                    enabled = hasSentences && !isFinalizing,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_phrase_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.cd_next_phrase),
                        tint = Color.White,
                    )
                }
            }
        }

        // Row 2: Separate Primary Recording Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!isRecordingActive && !isFinalizing) {
                OutlinedButton(
                    onClick = onStartShortTest,
                    enabled = cameraReady,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("short_test_record_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Hearing,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Teste curto",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Button(
                    onClick = onStartRecording,
                    enabled = cameraReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RecCrimson,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("start_recording_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FiberManualRecord,
                        contentDescription = stringResource(R.string.cd_record_toggle),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Gravar vídeo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                // Live mic indicator from CameraX AudioStats + Big Finalize Recording button
                val amplitude = (recordingState as? RecordingState.Recording)?.audioAmplitude ?: 0.0
                val hasAudio = (recordingState as? RecordingState.Recording)?.hasAudioTrack ?: true

                Surface(
                    color = Color(0xCC0F172A),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier.height(56.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Nível do microfone",
                            tint = if (hasAudio) StudioTealOnDark else Color(0xFFFCA5A5),
                            modifier = Modifier.size(18.dp),
                        )
                        LiveAudioLevelBars(amplitude = amplitude)
                    }
                }

                Button(
                    onClick = onStopRecording,
                    enabled = !isFinalizing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = RecCrimson,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("stop_recording_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = stringResource(R.string.cd_record_toggle),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isFinalizing) "Finalizando..." else "Finalizar gravação",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveAudioLevelBars(amplitude: Double) {
    // CameraX AudioStats.audioAmplitude is normalized [0.0, 1.0]
    val level = amplitude.coerceIn(0.0, 1.0).toFloat()
    val thresholds = listOf(0.05f, 0.20f, 0.45f, 0.70f)
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        thresholds.forEachIndexed { idx, threshold ->
            val active = level >= threshold
            val barHeight = (8 + idx * 4).dp
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(CircleShape)
                    .background(
                        if (active) StudioTealOnDark else Color.White.copy(alpha = 0.25f)
                    )
            )
        }
    }
}
