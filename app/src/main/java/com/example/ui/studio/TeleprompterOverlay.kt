package com.example.ui.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PanelHeightMode
import com.example.model.PlaybackMode
import com.example.model.PlaybackState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioTealOnDark

@Composable
fun TeleprompterOverlay(
    scriptTitle: String,
    sentences: List<String>,
    playbackState: PlaybackState,
    isExpanded: Boolean,
    panelHeightMode: PanelHeightMode,
    fontSizeSp: Float,
    onToggleExpanded: () -> Unit,
    onCycleHeight: () -> Unit,
    onFontSmaller: () -> Unit,
    onFontLarger: () -> Unit,
    onPhraseTapped: (Int) -> Unit,
) {
    val listState = rememberLazyListState()

    // Automatically follow the currently selected / speaking phrase
    LaunchedEffect(playbackState.currentIndex, isExpanded) {
        if (isExpanded && playbackState.currentIndex in sentences.indices) {
            val target = (playbackState.currentIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    val statusLabel = if (sentences.isEmpty()) {
        "Sem roteiro • Modo livre"
    } else {
        when (playbackState.mode) {
            PlaybackMode.IDLE -> "Pronto • ${playbackState.currentIndex + 1}/${sentences.size}"
            PlaybackMode.SPEAKING_CONTINUOUS ->
                if (playbackState.isRehearsal) "Ensaio • ${playbackState.currentIndex + 1}/${sentences.size}"
                else "Lendo • ${playbackState.currentIndex + 1}/${sentences.size}"
            PlaybackMode.WAITING_PAUSE -> "Pausa • ${playbackState.currentIndex + 1}/${sentences.size}"
            PlaybackMode.SPEAKING_SINGLE -> "Repetindo frase ${playbackState.currentIndex + 1}"
            PlaybackMode.PAUSED -> "Pausado • ${playbackState.currentIndex + 1}/${sentences.size}"
            PlaybackMode.COMPLETED -> "Leitura concluída"
        }
    }

    val statusColor = when (playbackState.mode) {
        PlaybackMode.SPEAKING_CONTINUOUS,
        PlaybackMode.WAITING_PAUSE,
        PlaybackMode.SPEAKING_SINGLE -> StudioTealOnDark
        PlaybackMode.COMPLETED -> Color(0xFF86EFAC)
        else -> Color(0xFFCBD5E1)
    }

    Surface(
        color = Color(0xB80B1219),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("teleprompter_overlay_panel"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Compact Header Row with status, font size, height cycle & collapse toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusLabel,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("playback_status_label"),
                    )
                    if (!isExpanded && scriptTitle.isNotBlank()) {
                        Text(
                            text = scriptTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (isExpanded) {
                    IconButton(
                        onClick = onFontSmaller,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("font_smaller_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TextDecrease,
                            contentDescription = stringResource(R.string.cd_font_smaller),
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onFontLarger,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("font_larger_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TextIncrease,
                            contentDescription = stringResource(R.string.cd_font_larger),
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onCycleHeight,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("panel_height_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Height,
                            contentDescription = stringResource(R.string.cd_panel_height),
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("toggle_panel_button"),
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.cd_toggle_panel),
                        tint = Color.White,
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                if (sentences.isEmpty()) {
                    Text(
                        text = "Nenhum roteiro carregado ainda. Você já pode testar a câmera e gravar livremente, ou voltar para colar suas frases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    )
                } else {
                    val maxPanelHeight = when (panelHeightMode) {
                        PanelHeightMode.COMPACT -> 150.dp
                        PanelHeightMode.MEDIUM -> 235.dp
                        PanelHeightMode.LARGE -> 340.dp
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = maxPanelHeight),
                        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(
                            items = sentences,
                            key = { idx, _ -> idx },
                        ) { index, sentence ->
                        val isCurrent = index == playbackState.currentIndex
                        val isCompleted = index < playbackState.currentIndex
                        val bg = if (isCurrent) {
                            Color(0xDD0F766E)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        }
                        val textColor = when {
                            isCurrent -> Color.White
                            isCompleted -> Color.White.copy(alpha = 0.52f)
                            else -> Color.White.copy(alpha = 0.84f)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bg)
                                .clickable { onPhraseTapped(index) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("studio_phrase_$index"),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = String.format("%02d", index + 1),
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) Color(0xFFCCFBF1) else Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.padding(top = 3.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = sentence,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.32f).sp,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = textColor,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
