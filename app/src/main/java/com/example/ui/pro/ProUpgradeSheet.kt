package com.example.ui.pro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.billing.ProAccessState
import com.example.billing.ProPlanChoice
import com.example.ui.theme.RouteAmber
import com.example.ui.theme.RouteAmberContainer
import com.example.ui.theme.RouteEmerald
import com.example.ui.theme.RouteEmeraldContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProUpgradeSheet(
    proState: ProAccessState,
    triggeredByRecordAttempt: Boolean,
    onPurchasePlan: (ProPlanChoice) -> Unit,
    onRestorePurchases: () -> Unit,
    onToggleDebugPro: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPlan by remember { mutableStateOf(ProPlanChoice.LIFETIME) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (proState.isProUnlocked) Icons.Filled.Verified else Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column {
                        Text(
                            text = if (proState.isProUnlocked) {
                                "Escuta e Grava ${proState.activePlanLabel ?: "Pro"}"
                            } else {
                                "Escuta e Grava Pro"
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = if (proState.isProUnlocked) {
                                "Gravação ilimitada liberada neste aparelho"
                            } else {
                                "Teste todas as funções grátis • Ative para gravar"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar")
                }
            }

            // Editorial Studio Hero Banner
            Image(
                painter = painterResource(id = R.drawable.img_pro_studio_banner),
                contentDescription = "Estúdio Escuta e Grava com celular, ondas sonoras e fone sem fio",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )

            // Context banner when triggered by tapping Record in Trial mode
            if (triggeredByRecordAttempt && !proState.isProUnlocked) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "No Modo Teste você usa o teleprompter, voz narrador no fone e todos os controles da câmera. Ative o Pro para gravar e salvar vídeos no celular.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // Pending payment notice (Pix / Boleto / store cash)
            if (proState.isPendingPayment) {
                Surface(
                    color = RouteAmberContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = RouteAmber)
                        Text(
                            text = "Seu pagamento está pendente de confirmação no Google Play. Assim que aprovado, o modo Pro será ativado automaticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RouteAmber,
                        )
                    }
                }
            }

            // Status message from Billing flow
            proState.statusMessage?.let { msg ->
                Surface(
                    color = if (proState.isProUnlocked) RouteEmeraldContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (proState.isProUnlocked) RouteEmerald else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            // Feature highlights
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "O que o Pro libera",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    ProBenefitRow("Gravação e salvamento ilimitado em HD / Full HD / 4K na galeria")
                    ProBenefitRow("Teste curto de áudio e vazamento do fone antes de gravar")
                    ProBenefitRow("Zero marca d'água, zero anúncios e 100% offline no celular")
                    ProBenefitRow("Todos os ajustes de lentes, abertura, ISO, foco e voz narrador")
                }
            }

            if (!proState.isProUnlocked) {
                // Plan Cards: Lifetime (Recommended) + Annual
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlanOptionCard(
                        title = "Acesso Vitalício",
                        price = proState.lifetimePriceLabel,
                        subtitle = "Pagamento único • Sem mensalidade ou renovação",
                        badge = "Mais vantajoso",
                        selected = selectedPlan == ProPlanChoice.LIFETIME,
                        onClick = { selectedPlan = ProPlanChoice.LIFETIME },
                        testTag = "plan_lifetime_card",
                    )

                    PlanOptionCard(
                        title = "Plano Anual",
                        price = proState.annualPriceLabel,
                        subtitle = "Renovação anual pelo Google Play • Cancele quando quiser",
                        badge = null,
                        selected = selectedPlan == ProPlanChoice.ANNUAL,
                        onClick = { selectedPlan = ProPlanChoice.ANNUAL },
                        testTag = "plan_annual_card",
                    )
                }

                Button(
                    onClick = { onPurchasePlan(selectedPlan) },
                    enabled = !proState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_purchase_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = when (selectedPlan) {
                            ProPlanChoice.LIFETIME -> "Desbloquear Vitalício (${proState.lifetimePriceLabel})"
                            ProPlanChoice.ANNUAL -> "Assinar Anual (${proState.annualPriceLabel})"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Secondary actions: Restore purchase + Privacy policy + Continue Trial
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onRestorePurchases,
                    enabled = !proState.isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("restore_purchases_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restaurar compra")
                }

                OutlinedButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Privacidade")
                }
            }

            if (!proState.isProUnlocked) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continue_trial_button"),
                ) {
                    Text("Continuar no Modo Teste (sem gravar vídeos)")
                }
            }

            // Strictly Debug-only developer switch (compiled out in Google Play Release AABs)
            if (BuildConfig.DEBUG) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simular Pro (somente Debug)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Atalho exclusivo da build de teste. Removido automaticamente na versão da Play Store.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = proState.debugProOverrideEnabled,
                            onCheckedChange = onToggleDebugPro,
                            modifier = Modifier.testTag("debug_pro_override_switch"),
                        )
                    }
                }
            }

            Text(
                text = "Pagamentos processados com segurança pelo Google Play. A assinatura anual pode ser gerenciada ou cancelada a qualquer momento nas configurações da Play Store.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacidade e Dados") },
            text = {
                Text(
                    "O Escuta e Grava funciona 100% no seu aparelho.\n\n" +
                        "• Seus roteiros ficam salvos apenas no armazenamento local do celular.\n" +
                        "• Seus vídeos são salvos diretamente na galeria do aparelho (Movies/EscutaEGrava) e nunca são enviados para servidores externos.\n" +
                        "• Câmera, microfone e Bluetooth são usados exclusivamente durante o ensaio e a gravação.\n" +
                        "• Assinaturas e compras são processadas integralmente pelo Google Play Billing — o app não coleta nem armazena dados de cartão ou conta bancária."
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Entendi")
                }
            },
        )
    }
}

@Composable
private fun ProBenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = RouteEmerald,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    price: String,
    subtitle: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        onClick = onClick,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (badge != null) {
                        Surface(
                            color = RouteEmeraldContainer,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = RouteEmerald,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = price,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
