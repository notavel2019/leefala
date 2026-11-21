package com.example.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.billing.ProAccessState
import com.example.model.AudioRouteInfo
import com.example.model.AudioRouteKind
import com.example.model.NarratorStyle
import com.example.model.ScriptDocument
import com.example.model.TeleprompterPreferences
import com.example.model.TtsAvailability
import com.example.model.UpdateCheckState
import com.example.model.VoiceOption
import com.example.ui.theme.RouteAmber
import com.example.ui.theme.RouteAmberContainer
import com.example.ui.theme.RouteEmerald
import com.example.ui.theme.RouteEmeraldContainer
import com.example.viewmodel.SetupUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptSetupScreen(
    setupState: SetupUiState,
    preferences: TeleprompterPreferences,
    audioRoute: AudioRouteInfo,
    ttsAvailability: TtsAvailability,
    voices: List<VoiceOption>,
    selectedVoiceName: String?,
    activeTtsEngineLabel: String,
    hasGoogleTtsInstalled: Boolean,
    isPlayingSample: Boolean,
    onTitleChanged: (String) -> Unit,
    onRawTextChanged: (String) -> Unit,
    onResplitFromRawText: () -> Unit,
    onUpdateSentence: (Int, String) -> Unit,
    onMergeWithNext: (Int) -> Unit,
    onSplitAtWord: (Int, Int) -> Unit,
    onRemoveSentence: (Int) -> Unit,
    onAddSentenceAfter: (Int, String) -> Unit,
    onSaveScript: () -> Unit,
    onOpenScript: (ScriptDocument) -> Unit,
    onCreateNewScript: () -> Unit,
    onDeleteScript: (String) -> Unit,
    onClearBanner: () -> Unit,
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
    onOpenStudio: () -> Unit,
    githubRepoSlug: String,
    updateCheckState: UpdateCheckState,
    onGithubRepoSlugChanged: (String) -> Unit,
    onCheckGithubUpdate: () -> Unit,
    onResetUpdateState: () -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    proAccessState: ProAccessState,
    onOpenProUpgrade: () -> Unit,
) {
    // 0 = Roteiro, 1 = Frases, 2 = Voz e Áudio
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSavedScriptsSheet by remember { mutableStateOf(false) }
    var showUpdateSheet by remember { mutableStateOf(false) }
    var showConfirmResplit by remember { mutableStateOf(false) }
    var showAddPhraseDialog by remember { mutableStateOf(false) }

    val script = setupState.currentScript
    val sentenceCount = script.sentences.size

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Teleprompter auditivo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenStudio,
                        modifier = Modifier.testTag("top_open_camera_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = "Abrir câmera",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (BuildConfig.DEBUG) {
                        IconButton(
                            onClick = { showUpdateSheet = true },
                            modifier = Modifier.testTag("open_update_sheet_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdate,
                                contentDescription = "Atualizar pelo GitHub (Debug)",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(
                        onClick = onCreateNewScript,
                        modifier = Modifier.testTag("new_script_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NoteAdd,
                            contentDescription = "Novo roteiro",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        onClick = { showSavedScriptsSheet = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("saved_scripts_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = stringResource(R.string.cd_saved_scripts),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${setupState.savedScripts.size}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.imePadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onSaveScript,
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("save_script_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            imageVector = if (setupState.hasUnsavedChanges) Icons.Filled.Save else Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (setupState.hasUnsavedChanges) "Salvar" else "Salvo")
                    }

                    Button(
                        onClick = onOpenStudio,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("open_studio_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (sentenceCount > 0) "Abrir câmera ($sentenceCount frases)" else "Abrir câmera",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp),
            ) {
                // Compact Live Audio Route Strip
                AudioRouteCompactStrip(
                    audioRoute = audioRoute,
                    onClick = { selectedTab = 2 },
                )

                Spacer(Modifier.height(8.dp))

                // Compact Trial / Pro Status Strip
                ProStatusCompactStrip(
                    proAccessState = proAccessState,
                    onClick = onOpenProUpgrade,
                )

                Spacer(Modifier.height(10.dp))

                // Feedback banner if any
                AnimatedVisibility(visible = setupState.statusBanner != null) {
                    setupState.statusBanner?.let { banner ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = banner,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = onClearBanner,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Fechar aviso",
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Clean 3-tab selector for airy, uncluttered experience
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_tabs"),
                ) {
                    val tabs = listOf(
                        Triple("Roteiro", Icons.Outlined.EditNote, "tab_script"),
                        Triple("Frases ($sentenceCount)", Icons.AutoMirrored.Filled.FormatListBulleted, "tab_phrases"),
                        Triple("Voz e Áudio", Icons.Outlined.RecordVoiceOver, "tab_voice"),
                    )
                    tabs.forEachIndexed { idx, (label, icon, tag) ->
                        SegmentedButton(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                            modifier = Modifier.testTag(tag),
                            icon = {},
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> ScriptEditorTab(
                        script = script,
                        onTitleChanged = onTitleChanged,
                        onRawTextChanged = onRawTextChanged,
                        onOpenPhrasesTab = { selectedTab = 1 },
                        onRequestResplit = {
                            if (script.hasManualSplitEdits) {
                                showConfirmResplit = true
                            } else {
                                onResplitFromRawText()
                            }
                        },
                    )

                    1 -> SentencesReviewTab(
                        script = script,
                        onUpdateSentence = onUpdateSentence,
                        onMergeWithNext = onMergeWithNext,
                        onSplitAtWord = onSplitAtWord,
                        onRemoveSentence = onRemoveSentence,
                        onAddSentenceClick = { showAddPhraseDialog = true },
                        onRequestResplit = {
                            if (script.hasManualSplitEdits) {
                                showConfirmResplit = true
                            } else {
                                onResplitFromRawText()
                            }
                        },
                    )

                    2 -> VoiceAudioPanel(
                        preferences = preferences,
                        audioRoute = audioRoute,
                        ttsAvailability = ttsAvailability,
                        voices = voices,
                        selectedVoiceName = selectedVoiceName,
                        activeTtsEngineLabel = activeTtsEngineLabel,
                        hasGoogleTtsInstalled = hasGoogleTtsInstalled,
                        isPlayingSample = isPlayingSample,
                        onSelectVoice = onSelectVoice,
                        onApplyNarratorStyle = onApplyNarratorStyle,
                        onSpeechRateChange = onSpeechRateChange,
                        onSpeechPitchChange = onSpeechPitchChange,
                        onPauseSecondsChange = onPauseSecondsChange,
                        onNaturalProsodyToggle = onNaturalProsodyToggle,
                        onPreferGoogleTtsToggle = onPreferGoogleTtsToggle,
                        onReturnVolumeChange = onReturnVolumeChange,
                        onStepReturnVolume = onStepReturnVolume,
                        onToggleSample = onToggleSample,
                        onRefreshTtsAndRoute = onRefreshTtsAndRoute,
                        onOpenBluetoothSettings = onOpenBluetoothSettings,
                        onInstallTtsData = onInstallTtsData,
                        onOpenTtsSettings = onOpenTtsSettings,
                    )
                }
            }
        }
    }

    if (BuildConfig.DEBUG && showUpdateSheet) {
        UpdateSheet(
            repoSlug = githubRepoSlug,
            updateState = updateCheckState,
            onRepoSlugChanged = onGithubRepoSlugChanged,
            onCheckUpdate = onCheckGithubUpdate,
            onOpenUrl = onOpenExternalUrl,
            onDismiss = {
                showUpdateSheet = false
                onResetUpdateState()
            },
        )
    }

    if (showSavedScriptsSheet) {
        SavedScriptsSheet(
            scripts = setupState.savedScripts,
            activeScriptId = script.id,
            onDismiss = { showSavedScriptsSheet = false },
            onOpenScript = {
                onOpenScript(it)
                showSavedScriptsSheet = false
            },
            onCreateNew = {
                onCreateNewScript()
                showSavedScriptsSheet = false
                selectedTab = 0
            },
            onDeleteScript = onDeleteScript,
        )
    }

    if (showConfirmResplit) {
        AlertDialog(
            onDismissRequest = { showConfirmResplit = false },
            title = { Text("Re-dividir frases?") },
            text = {
                Text(
                    "Você fez ajustes manuais nas frases deste roteiro. Re-dividir a partir do texto original vai substituir esses ajustes."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmResplit = false
                        onResplitFromRawText()
                    },
                ) {
                    Text("Re-dividir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResplit = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showAddPhraseDialog) {
        var newPhraseText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPhraseDialog = false },
            title = { Text("Adicionar frase ao final") },
            text = {
                OutlinedTextField(
                    value = newPhraseText,
                    onValueChange = { newPhraseText = it },
                    placeholder = { Text("Digite a frase...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                )
            },
            confirmButton = {
                Button(
                    enabled = newPhraseText.isNotBlank(),
                    onClick = {
                        onAddSentenceAfter(script.sentences.lastIndex, newPhraseText)
                        showAddPhraseDialog = false
                    },
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPhraseDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun AudioRouteCompactStrip(
    audioRoute: AudioRouteInfo,
    onClick: () -> Unit,
) {
    val (bg, fg, icon) = when (audioRoute.kind) {
        AudioRouteKind.BLUETOOTH_MEDIA -> Triple(RouteEmeraldContainer, RouteEmerald, Icons.Filled.BluetoothAudio)
        AudioRouteKind.WIRED_HEADSET -> Triple(RouteEmeraldContainer, RouteEmerald, Icons.Filled.Headphones)
        AudioRouteKind.BLUETOOTH_CALL_SCO -> Triple(RouteAmberContainer, RouteAmber, Icons.Filled.BluetoothAudio)
        AudioRouteKind.PHONE_SPEAKER,
        AudioRouteKind.UNCONFIRMED -> Triple(RouteAmberContainer, RouteAmber, Icons.Filled.VolumeUp)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("audio_route_strip"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(fg.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.cd_audio_route),
                    tint = fg,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = audioRoute.description,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = fg,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Ajustar",
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProStatusCompactStrip(
    proAccessState: ProAccessState,
    onClick: () -> Unit,
) {
    val unlocked = proAccessState.isProUnlocked
    Surface(
        color = if (unlocked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("pro_status_strip"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (unlocked) Icons.Filled.Verified else Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = if (unlocked) {
                    "${proAccessState.activePlanLabel ?: "Pro"} ativo • Gravação de vídeo liberada"
                } else {
                    "Modo Teste gratuito • Teste tudo e ative o Pro para gravar"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (unlocked) "Detalhes" else "Ver planos",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ScriptEditorTab(
    script: ScriptDocument,
    onTitleChanged: (String) -> Unit,
    onRawTextChanged: (String) -> Unit,
    onOpenPhrasesTab: () -> Unit,
    onRequestResplit: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = script.title,
                        onValueChange = onTitleChanged,
                        label = { Text("Título do roteiro") },
                        placeholder = { Text("Ex.: Mensagem sobre Jo 3:16") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("script_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )

                    OutlinedTextField(
                        value = script.rawText,
                        onValueChange = onRawTextChanged,
                        label = { Text("Texto do roteiro (pt-BR)") },
                        placeholder = {
                            Text(
                                "Cole ou escreva seu roteiro aqui. O app divide automaticamente em frases preservando acentos, números e referências bíblicas."
                            )
                        },
                        minLines = 8,
                        maxLines = 18,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("script_body_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )

                    if (script.hasManualSplitEdits) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Divisão manual ativa (${script.sentences.size} frases).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = onRequestResplit) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Re-dividir")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (script.sentences.isEmpty()) {
                                "Nenhuma frase gerada ainda"
                            } else {
                                "${script.sentences.size} frases prontas para leitura"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (script.sentences.isNotEmpty()) {
                            TextButton(
                                onClick = onOpenPhrasesTab,
                                modifier = Modifier.testTag("review_phrases_button"),
                            ) {
                                Text("Revisar divisão")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SentencesReviewTab(
    script: ScriptDocument,
    onUpdateSentence: (Int, String) -> Unit,
    onMergeWithNext: (Int) -> Unit,
    onSplitAtWord: (Int, Int) -> Unit,
    onRemoveSentence: (Int) -> Unit,
    onAddSentenceClick: () -> Unit,
    onRequestResplit: () -> Unit,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var splittingIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ajuste abreviações, números ou pausas antes de gravar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onAddSentenceClick) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Frase")
                }
                if (script.rawText.isNotBlank()) {
                    TextButton(onClick = onRequestResplit) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Auto")
                    }
                }
            }
        }

        if (script.sentences.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = "Nenhuma frase ainda",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Cole seu roteiro na aba Roteiro para gerar automaticamente a lista de frases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(
                    items = script.sentences,
                    key = { index, text -> "${index}_${text.hashCode()}" },
                ) { index, sentence ->
                    SentenceEditableCard(
                        index = index,
                        total = script.sentences.size,
                        text = sentence,
                        canMergeNext = index < script.sentences.lastIndex,
                        canSplit = sentence.trim().split(Regex("\\s+")).size >= 2,
                        onEdit = { editingIndex = index },
                        onSplit = { splittingIndex = index },
                        onMergeNext = { onMergeWithNext(index) },
                        onDelete = { onRemoveSentence(index) },
                    )
                }
            }
        }
    }

    editingIndex?.let { idx ->
        val original = script.sentences.getOrNull(idx)
        if (original != null) {
            EditSentenceDialog(
                index = idx,
                initialText = original,
                onDismiss = { editingIndex = null },
                onSave = { updated ->
                    onUpdateSentence(idx, updated)
                    editingIndex = null
                },
            )
        }
    }

    splittingIndex?.let { idx ->
        val original = script.sentences.getOrNull(idx)
        if (original != null) {
            SplitSentenceDialog(
                index = idx,
                sentence = original,
                onDismiss = { splittingIndex = null },
                onConfirmSplit = { wordIdx ->
                    onSplitAtWord(idx, wordIdx)
                    splittingIndex = null
                },
            )
        }
    }
}
