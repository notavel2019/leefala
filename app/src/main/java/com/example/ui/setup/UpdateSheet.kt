package com.example.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.model.UpdateCheckState
import com.example.ui.theme.RouteEmerald
import com.example.ui.theme.RouteEmeraldContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(
    repoSlug: String,
    updateState: UpdateCheckState,
    onRepoSlugChanged: (String) -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
                Column {
                    Text(
                        text = "Atualizar pelo GitHub",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Versão instalada: v${BuildConfig.VERSION_NAME} (código ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "Sempre que você enviar alterações do AI Studio para o GitHub, o workflow gera o EscutaEGrava.apk com a mesma assinatura na Release 'latest' — atualiza direto por cima sem apagar seus roteiros.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = repoSlug,
                onValueChange = onRepoSlugChanged,
                label = { Text("Repositório GitHub") },
                placeholder = { Text("Ex.: notavel2019/escuta-e-grava") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("github_repo_input"),
                shape = RoundedCornerShape(12.dp),
            )

            Button(
                onClick = onCheckUpdate,
                enabled = repoSlug.isNotBlank() && updateState !is UpdateCheckState.Checking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("check_github_update_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (updateState is UpdateCheckState.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Verificando no GitHub...")
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Verificar atualização agora")
                }
            }

            when (updateState) {
                is UpdateCheckState.UpdateAvailable -> {
                    Surface(
                        color = RouteEmeraldContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, RouteEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Nova versão disponível: v${updateState.latestVersion}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RouteEmerald,
                            )
                            Text(
                                text = updateState.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { onOpenUrl(updateState.downloadUrl) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("download_apk_update_button"),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Baixar EscutaEGrava.apk no navegador")
                            }
                        }
                    }
                }

                is UpdateCheckState.UpToDate -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RouteEmerald)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Você já está na versão mais recente (v${updateState.currentVersion}).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                is UpdateCheckState.NeedsBrowserAccess -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = updateState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenUrl(updateState.releasesUrl) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Releases")
                                }
                                OutlinedButton(
                                    onClick = { onOpenUrl(updateState.actionsUrl) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Actions")
                                }
                            }
                        }
                    }
                }

                else -> Unit
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
