package com.example.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AudioRouteInfo
import com.example.model.AudioRouteKind
import com.example.model.TeleprompterPreferences
import com.example.model.TtsAvailability
import com.example.model.VoiceOption
import com.example.ui.theme.RouteAmber
import com.example.ui.theme.RouteAmberContainer
import com.example.ui.theme.RouteEmerald
import com.example.ui.theme.RouteEmeraldContainer
import java.util.Locale

@Composable
fun VoiceAudioPanel(
    preferences: TeleprompterPreferences,
    audioRoute: AudioRouteInfo,
    ttsAvailability: TtsAvailability,
    voices: List<VoiceOption>,
    selectedVoiceName: String?,
    isPlayingSample: Boolean,
    onSelectVoice: (String) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onPauseSecondsChange: (Float) -> Unit,
    onToggleSample: () -> Unit,
    onRefreshTtsAndRoute: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onInstallTtsData: () -> Unit,
    onOpenTtsSettings: () -> Unit,
) {
    val selectedVoice = voices.firstOrNull { it.name == selectedVoiceName }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // Card 1: Bluetooth & Audio Route + Honest Mic Info
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Fone Bluetooth e áudio",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(
                            onClick = onRefreshTtsAndRoute,
                            modifier = Modifier.testTag("refresh_audio_route_button"),
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Verificar")
                        }
                    }

                    val isSafe = audioRoute.isHeadsetSafe
                    Surface(
                        color = if (isSafe) RouteEmeraldContainer else RouteAmberContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = when (audioRoute.kind) {
                                    AudioRouteKind.BLUETOOTH_MEDIA,
                                    AudioRouteKind.BLUETOOTH_CALL_SCO -> Icons.Filled.BluetoothAudio
                                    AudioRouteKind.WIRED_HEADSET -> Icons.Filled.Headphones
                                    else -> Icons.Filled.VolumeUp
                                },
                                contentDescription = null,
                                tint = if (isSafe) RouteEmerald else RouteAmber,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = audioRoute.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSafe) RouteEmerald else RouteAmber,
                                )
                                Text(
                                    text = if (isSafe) {
                                        "Saída de mídia (sem modo chamada/SCO). Se o fone desconectar, a leitura pausa na hora."
                                    } else {
                                        "Conecte seu fone Bluetooth nas configurações do Android para ouvir o guia sem vazar no vídeo."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(18.dp),
                        )
                        Text(
                            text = "Microfone: entrada padrão do sistema (o app não mixa o TTS no vídeo). Use o botão \"Teste curto\" na tela de gravação para conferir a captação e eventual vazamento do fone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenBluetoothSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_bluetooth_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir configurações de Bluetooth")
                    }
                }
            }
        }

        // Card 2: Speed & Pause Sliders + Sample Button
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Ritmo da voz guia",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    // Speed: 0.6x to 1.2x (default 0.9x)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Velocidade da fala", style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = formatPtDecimal(preferences.speechRate, "x"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                        Slider(
                            value = preferences.speechRate,
                            onValueChange = onSpeechRateChange,
                            valueRange = 0.6f..1.2f,
                            steps = 11,
                            modifier = Modifier.testTag("speech_rate_slider"),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("0,6x (mais lenta)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("0,9x padrão", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("1,2x", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Pause: 0.0s to 5.0s in 0.5s steps (default 1.0s)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Pausa entre frases", style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = formatPtDecimal(preferences.pauseSeconds, "s"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                        Slider(
                            value = preferences.pauseSeconds,
                            onValueChange = onPauseSecondsChange,
                            valueRange = 0f..5f,
                            steps = 9,
                            modifier = Modifier.testTag("pause_seconds_slider"),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("0,0s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("1,0s padrão", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("5,0s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Button(
                        onClick = onToggleSample,
                        enabled = ttsAvailability == TtsAvailability.READY,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("play_voice_sample_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlayingSample) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isPlayingSample) "Parar amostra" else "Ouvir amostra de voz")
                    }
                }
            }
        }

        // Card 3: pt-BR Voice Picker & Offline / Install status
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Voz em Português (pt-BR)",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    when (ttsAvailability) {
                        TtsAvailability.INITIALIZING -> {
                            Text(
                                text = "Carregando motor de voz do Android...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        TtsAvailability.PT_BR_MISSING,
                        TtsAvailability.ENGINE_ERROR -> {
                            Surface(
                                color = RouteAmberContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.WarningAmber,
                                            contentDescription = null,
                                            tint = RouteAmber,
                                        )
                                        Text(
                                            text = if (ttsAvailability == TtsAvailability.PT_BR_MISSING) {
                                                "Dados de voz em Português (Brasil) não encontrados."
                                            } else {
                                                "Não foi possível inicializar o motor de voz do aparelho."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = RouteAmber,
                                        )
                                    }
                                    Text(
                                        text = "Instale o pacote de voz pt-BR ou escolha outro motor de conversão de texto em voz nas configurações do Android.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        FilledTonalButton(
                                            onClick = onInstallTtsData,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Instalar voz")
                                        }
                                        OutlinedButton(
                                            onClick = onOpenTtsSettings,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Configurar")
                                        }
                                    }
                                }
                            }
                        }

                        TtsAvailability.READY -> {
                            // Offline / Airplane mode note
                            if (selectedVoice != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (selectedVoice.requiresNetwork) Icons.Filled.Cloud else Icons.Filled.OfflinePin,
                                            contentDescription = null,
                                            tint = if (selectedVoice.requiresNetwork) RouteAmber else RouteEmerald,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = if (selectedVoice.requiresNetwork) {
                                                "A voz selecionada exige internet. Para gravar em modo avião, escolha uma voz marcada como Offline."
                                            } else {
                                                "Voz instalada localmente (Offline) — pronta para testar em modo avião."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            if (voices.isEmpty()) {
                                Text(
                                    text = "Usando a voz padrão pt-BR do sistema (o motor atual não expôs uma lista detalhada de vozes).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    voices.forEach { voice ->
                                        val isSelected = voice.name == selectedVoiceName
                                        Surface(
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelectVoice(voice.name) },
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { onSelectVoice(voice.name) },
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = voice.displayName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    )
                                                    Text(
                                                        text = if (voice.requiresNetwork) {
                                                            "Exige rede • ${voice.name}"
                                                        } else {
                                                            "Offline no aparelho • ${voice.name}"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = onOpenTtsSettings) {
                                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Motor TTS do Android")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatPtDecimal(value: Float, suffix: String): String {
    return String.format(Locale("pt", "BR"), "%.1f%s", value, suffix)
}
