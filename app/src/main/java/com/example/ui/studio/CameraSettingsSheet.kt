package com.example.ui.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.example.model.CameraHardwareCapabilities
import com.example.model.CameraSettingsState
import com.example.model.WhiteBalancePreset
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CameraSettingsSheet(
    capabilities: CameraHardwareCapabilities,
    settings: CameraSettingsState,
    effectiveQualityLabel: String,
    isRecordingLocked: Boolean,
    onSelectLens: (cameraId: String, isFront: Boolean) -> Unit,
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
    onResetToAuto: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Configurações da câmera",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                TextButton(
                    onClick = onResetToAuto,
                    modifier = Modifier.testTag("camera_reset_auto_button"),
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Auto")
                }
            }

            if (isRecordingLocked) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = "Gravando: troca de lente, resolução e FPS ficam travados. Zoom, exposição e balanço de branco continuam ativos.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // 1. Lentes disponíveis no aparelho
            if (capabilities.availableLenses.isNotEmpty()) {
                SectionLabel("Lentes disponíveis")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    capabilities.availableLenses.forEach { lens ->
                        val isSelected = lens.cameraId == (settings.selectedCameraId ?: capabilities.activeLens?.cameraId)
                        FilterChip(
                            selected = isSelected,
                            enabled = !isRecordingLocked,
                            onClick = { onSelectLens(lens.cameraId, lens.isFront) },
                            label = { Text(lens.label, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("lens_chip_${lens.cameraId}"),
                        )
                    }
                }
            }

            // 2. Abertura (f/) — real hardware query
            SectionLabel("Abertura da lente (f/)")
            if (capabilities.supportsVariableAperture) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    capabilities.availableApertures.forEach { fNumber ->
                        val selected = settings.selectedAperture == fNumber
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectAperture(fNumber) },
                            label = {
                                Text(
                                    text = String.format(Locale.US, "f/%.1f", fNumber),
                                    fontFamily = JetBrainsMonoFamily,
                                )
                            },
                            modifier = Modifier.testTag("aperture_chip_$fNumber"),
                        )
                    }
                }
            } else {
                val fixedText = capabilities.availableApertures.firstOrNull()?.let { f ->
                    String.format(Locale.US, "f/%.1f (abertura física fixa desta lente)", f)
                } ?: "Abertura fixa informada pelo driver da câmera"
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Camera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Column {
                            Text(
                                text = fixedText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "A maioria dos celulares possui abertura física fixa; controle a luz abaixo em Exposição (EV) ou ISO/Obturador.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // 3. Zoom óptico / digital (permite acessar ultra-wide 0.5x/0.6x e tele 2x/3x em câmeras lógicas)
            if (capabilities.maxZoomRatio > capabilities.minZoomRatio + 0.05f) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("Zoom / Enquadramento")
                    Text(
                        text = String.format(Locale("pt", "BR"), "%.1fx", settings.zoomRatio),
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                val presets = listOf(0.5f, 0.6f, 1.0f, 2.0f, 3.0f, 5.0f).filter {
                    it >= capabilities.minZoomRatio - 0.02f && it <= capabilities.maxZoomRatio + 0.02f
                }
                if (presets.size >= 2) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { preset ->
                            val isNear = kotlin.math.abs(settings.zoomRatio - preset) < 0.08f
                            FilterChip(
                                selected = isNear,
                                onClick = { onZoomChange(preset) },
                                label = {
                                    Text(
                                        text = if (preset == preset.roundToInt().toFloat()) {
                                            "${preset.roundToInt()}x"
                                        } else {
                                            String.format(Locale("pt", "BR"), "%.1fx", preset)
                                        },
                                        fontFamily = JetBrainsMonoFamily,
                                    )
                                },
                            )
                        }
                    }
                }
                Slider(
                    value = settings.zoomRatio.coerceIn(capabilities.minZoomRatio, capabilities.maxZoomRatio),
                    onValueChange = onZoomChange,
                    valueRange = capabilities.minZoomRatio..capabilities.maxZoomRatio.coerceAtMost(8f),
                    modifier = Modifier.testTag("camera_zoom_slider"),
                )
            }

            // 4. Exposição (EV) e Modo Manual (ISO + Obturador)
            if (capabilities.supportsExposureCompensation || capabilities.supportsManualExposure) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("Exposição e Luz")
                    if (capabilities.supportsManualExposure) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Manual (ISO)", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = settings.manualExposureEnabled,
                                onCheckedChange = onManualExposureToggle,
                                modifier = Modifier.testTag("manual_exposure_switch"),
                            )
                        }
                    }
                }

                if (settings.manualExposureEnabled && capabilities.supportsManualExposure) {
                    // Manual ISO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("ISO", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "ISO ${settings.manualIso}",
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Slider(
                        value = settings.manualIso.toFloat().coerceIn(
                            capabilities.isoMin.toFloat(),
                            capabilities.isoMax.toFloat(),
                        ),
                        onValueChange = { onManualIsoChange(it.roundToInt()) },
                        valueRange = capabilities.isoMin.toFloat()..capabilities.isoMax.toFloat(),
                        modifier = Modifier.testTag("camera_iso_slider"),
                    )

                    // Manual Shutter Speed presets
                    Text("Obturador (velocidade)", style = MaterialTheme.typography.bodyMedium)
                    val shutterPresets = listOf(
                        "1/30s" to 33_333_333L,
                        "1/60s" to 16_666_666L,
                        "1/120s" to 8_333_333L,
                        "1/250s" to 4_000_000L,
                        "1/500s" to 2_000_000L,
                        "1/1000s" to 1_000_000L,
                    ).filter { (_, ns) -> ns in capabilities.shutterMinNs..capabilities.shutterMaxNs }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        shutterPresets.forEach { (label, ns) ->
                            val selected = kotlin.math.abs(settings.manualShutterNs - ns) < 400_000L
                            FilterChip(
                                selected = selected,
                                onClick = { onManualShutterChange(ns) },
                                label = { Text(label, fontFamily = JetBrainsMonoFamily) },
                            )
                        }
                    }
                } else if (capabilities.supportsExposureCompensation) {
                    val evValue = settings.exposureIndex * capabilities.exposureStepEv
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Compensação de exposição (EV)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = String.format(Locale("pt", "BR"), "%+.1f EV", evValue),
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Slider(
                        value = settings.exposureIndex.toFloat().coerceIn(
                            capabilities.exposureMinIndex.toFloat(),
                            capabilities.exposureMaxIndex.toFloat(),
                        ),
                        onValueChange = { onExposureIndexChange(it.roundToInt()) },
                        valueRange = capabilities.exposureMinIndex.toFloat()..capabilities.exposureMaxIndex.toFloat(),
                        modifier = Modifier.testTag("camera_ev_slider"),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Travar exposição (AE Lock)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Evita que o brilho oscile enquanto você fala",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.aeLocked,
                            onCheckedChange = onAeLockToggle,
                            modifier = Modifier.testTag("ae_lock_switch"),
                        )
                    }
                }
            }

            // 5. Balanço de Branco (WB)
            if (capabilities.supportedWhiteBalance.size > 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SectionLabel("Balanço de branco")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    capabilities.supportedWhiteBalance.forEach { preset ->
                        FilterChip(
                            selected = settings.whiteBalance == preset,
                            onClick = { onSelectWhiteBalance(preset) },
                            label = { Text(preset.label, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
            }

            // 6. Resolução, FPS, Estabilização e Lanterna
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionLabel("Vídeo e Estabilização")

            if (capabilities.supportedQualities.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    capabilities.supportedQualities.forEach { qLabel ->
                        val activeLabel = settings.selectedQualityLabel ?: effectiveQualityLabel
                        FilterChip(
                            selected = activeLabel == qLabel,
                            enabled = !isRecordingLocked,
                            onClick = { onSelectQuality(qLabel) },
                            label = { Text(qLabel, fontFamily = JetBrainsMonoFamily) },
                        )
                    }
                }
            }

            if (capabilities.supportedFps.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Taxa de quadros:", style = MaterialTheme.typography.bodyMedium)
                    capabilities.supportedFps.forEach { fps ->
                        FilterChip(
                            selected = settings.selectedFps == fps,
                            enabled = !isRecordingLocked,
                            onClick = { onSelectFps(fps) },
                            label = { Text("${fps} fps", fontFamily = JetBrainsMonoFamily) },
                        )
                    }
                }
            }

            if (capabilities.supportsVideoStabilization) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Estabilização de vídeo", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.videoStabilizationEnabled,
                        enabled = !isRecordingLocked,
                        onCheckedChange = onVideoStabilizationToggle,
                    )
                }
            }

            if (capabilities.hasFlashUnit) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.FlashlightOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Luz contínua / Lanterna", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = settings.torchEnabled,
                            onCheckedChange = onTorchToggle,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
