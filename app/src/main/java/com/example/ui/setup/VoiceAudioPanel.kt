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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.model.NarratorStyle
import com.example.model.TeleprompterPreferences
import com.example.model.TtsAvailability
import com.example.model.VoiceOption
import com.example.ui.theme.RouteAmber
import com.example.ui.theme.RouteAmberContainer
import com.example.ui.theme.RouteEmerald
import com.example.ui.theme.RouteEmeraldContainer
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun VoiceAudioPanel(
    preferences: TeleprompterPreferences,
    audioRoute: AudioRouteInfo,
    ttsAvailability: TtsAvailability,
    voices: List<VoiceOption>,
    selectedVoiceName: String?,
    activeTtsEngineLabel: String,
    hasGoogleTtsInstalled: Boolean,
    isPlayingSample: Boolean,
    onSelectVoice: (String) -> Unit,
    onApplyNarratorStyle: (NarratorStyle) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechPitchChange: (Float) -> Unit,
    onPauseSecondsChange: (Float) -> Unit,
    onNaturalProsodyToggle: (Boolean) -> Unit,
    onPreferGoogleTtsToggle: (Boolean) -> Unit,
    onReturnVolumeChange: (Float) -> Unit,
    onStepReturnVolume: (Float) -> Unit,
    onToggleSample: () -> Unit,
    onRefreshTtsAndRoute: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onInstallTtsData: () -> Unit,
    onOpenTtsSettings: () -> Unit,
) {
    val selectedVoice = voices.firstOrNull { it.name == selectedVoiceName }
    val volumePercent = (preferences.returnVolume * 100f).roundToInt().coerceIn(0, 100)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // Card 1: Return Volume Control (always easy to find & adjust)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconButton(
                                onClick = {
                                    onReturnVolumeChange(if (preferences.returnVolume > 0.01f) 0f else 0.70f)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("setup_volume_mute_button"),
                            ) {
                                Icon(
                                    imageVector = when {
                                        volumePercent == 0 -> Icons.Filled.VolumeOff
                                        volumePercent < 45 -> Icons.Filled.VolumeDown
                                        else -> Icons.Filled.VolumeUp
                                    },
                                    contentDescription = "Silenciar ou restaurar retorno",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Column {
                                Text(
                                    text = "Volume do retorno",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "Som que vai para o seu fone (também disponível na tela da câmera)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = if (volumePercent == 0) "Mudo" else "$volumePercent%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalIconButton(
                            onClick = { onStepReturnVolume(-0.10f) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("setup_volume_down_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Diminuir volume do retorno",
                            )
                        }

                        Slider(
                            value = preferences.returnVolume,
                            onValueChange = onReturnVolumeChange,
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("setup_volume_slider"),
                        )

                        FilledTonalIconButton(
                            onClick = { onStepReturnVolume(0.10f) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("setup_volume_up_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Aumentar volume do retorno",
                            )
                        }
                    }

                    Text(
                        text = "Dica: níveis abaixo de 15% aplicam atenuação fina para evitar que o áudio do fone vaze no microfone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Card 2: Narrator Style, Prosody, Pitch, Speed & Pause + Sample Button
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Estilo de narrador e fluidez",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    // Preset style chips
                    val selectableStyles = listOf(
                        NarratorStyle.NARRATOR_FLUID,
                        NarratorStyle.ANNOUNCER_DEEP,
                        NarratorStyle.CONVERSATIONAL,
                        NarratorStyle.DIDACTIC,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(selectableStyles) { style ->
                            val selected = preferences.narratorStyle == style
                            Surface(
                                onClick = { onApplyNarratorStyle(style) },
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                ),
                                modifier = Modifier.testTag("narrator_style_${style.name.lowercase(Locale.ROOT)}"),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = style.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    Text(
                                        text = style.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // Natural Prosody Switch
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Locução fluida (prosódia natural)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "Suaviza pontuações, expande abreviações e evita soletrar palavras em MAIÚSCULAS.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = preferences.naturalProsodyEnabled,
                                onCheckedChange = onNaturalProsodyToggle,
                                modifier = Modifier.testTag("natural_prosody_switch"),
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Pitch: 0.75 to 1.20 (warmer narrator tone around 0.90–0.96)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Tom da voz (grave / agudo)", style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = formatPtPitch(preferences.speechPitch),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                        Slider(
                            value = preferences.speechPitch,
                            onValueChange = onSpeechPitchChange,
                            valueRange = 0.75f..1.20f,
                            modifier = Modifier.testTag("speech_pitch_slider"),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Mais grave", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Narrador (0,94)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Mais agudo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Speed: 0.6x to 1.3x
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
                            valueRange = 0.6f..1.3f,
                            steps = 13,
                            modifier = Modifier.testTag("speech_rate_slider"),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("0,6x (lenta)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("1,0x natural", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("1,3x", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Pause: 0.0s to 5.0s in 0.5s steps
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
                            Text("0,8s–1,0s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        // Card 3: pt-BR Voice Picker, Google Engine Toggle & Better Voices Install
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voz em Português (pt-BR)",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Motor ativo: $activeTtsEngineLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (hasGoogleTtsInstalled) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Priorizar motor Google (vozes naturais)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "Costuma soar muito menos robótico do que motores padrão de fábrica.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = preferences.preferGoogleTtsEngine,
                                    onCheckedChange = onPreferGoogleTtsToggle,
                                    modifier = Modifier.testTag("prefer_google_tts_switch"),
                                )
                            }
                        }
                    }

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
                                        text = "Instale o pacote de voz pt-BR ou escolha o motor Serviços de fala do Google nas configurações do Android.",
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
                                                "Voz neural online ativa (mais natural com internet). Para gravar em modo avião, escolha uma voz marcada como Offline."
                                            } else {
                                                "Voz instalada localmente (Offline) — pronta inclusive em modo avião."
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
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { onSelectVoice(voice.name) },
                                                )
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    ) {
                                                        Text(
                                                            text = voice.displayName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                        )
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    ) {
                                                        if (voice.qualityBadge.isNotBlank()) {
                                                            Surface(
                                                                color = if (voice.isNaturalRecommended) {
                                                                    RouteEmeraldContainer
                                                                } else {
                                                                    MaterialTheme.colorScheme.surfaceVariant
                                                                },
                                                                shape = RoundedCornerShape(6.dp),
                                                            ) {
                                                                Text(
                                                                    text = voice.qualityBadge,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = if (voice.isNaturalRecommended) {
                                                                        RouteEmerald
                                                                    } else {
                                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                    },
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = voice.name,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onInstallTtsData,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Baixar vozes")
                                }
                                OutlinedButton(
                                    onClick = onOpenTtsSettings,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Motor TTS")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 4: Bluetooth & Audio Route + Honest Mic Info
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
    }
}

fun formatPtDecimal(value: Float, suffix: String): String {
    return String.format(Locale("pt", "BR"), "%.1f%s", value, suffix)
}

private fun formatPtPitch(value: Float): String {
    val num = String.format(Locale("pt", "BR"), "%.2f", value)
    return when {
        value < 0.90f -> "$num (Grave)"
        value <= 0.98f -> "$num (Narrador)"
        value <= 1.05f -> "$num (Neutro)"
        else -> "$num (Agudo)"
    }
}
