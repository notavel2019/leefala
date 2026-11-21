package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ProPlanChoice {
    ANNUAL,
    LIFETIME,
}

data class ProAccessState(
    val isProUnlocked: Boolean = false,
    val activePlanLabel: String? = null,
    val isPendingPayment: Boolean = false,
    val annualPriceLabel: String = "R$ 25,00 / ano",
    val lifetimePriceLabel: String = "R$ 50,00",
    val storeCatalogReady: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val debugProOverrideEnabled: Boolean = false,
)

/**
 * Real Google Play Billing Library 8.0.0 integration for Escuta e Grava Pro.
 *
 * Security & policy guarantees:
 * - Grants Pro ONLY when Google Play reports PurchaseState.PURCHASED (never on PENDING).
 * - Automatically acknowledges unacknowledged PURCHASED transactions.
 * - Uses separate ProductType.SUBS and ProductType.INAPP queries per Billing Library 8.x spec.
 * - Debug preview override is strictly gated behind BuildConfig.DEBUG and compiled out of Release AABs.
 */
class PlayBillingManager(context: Context) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(ProAccessState())
    val state: StateFlow<ProAccessState> = _state.asStateFlow()

    @Volatile
    private var annualProductDetails: ProductDetails? = null

    @Volatile
    private var lifetimeProductDetails: ProductDetails? = null

    @Volatile
    private var verifiedAnnualOwned: Boolean = false

    @Volatile
    private var verifiedLifetimeOwned: Boolean = false

    @Volatile
    private var pendingPurchaseDetected: Boolean = false

    @Volatile
    private var debugOverrideActive: Boolean = false

    private var billingClient: BillingClient? = null

    init {
        initClientAndConnect()
    }

    private fun initClientAndConnect() {
        try {
            val client = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .enablePrepaidPlans()
                        .build()
                )
                .enableAutoServiceReconnection()
                .build()
            billingClient = client
            startConnection()
        } catch (e: Exception) {
            Log.w(TAG, "Google Play Billing indisponível neste ambiente: ${e.javaClass.simpleName}")
        }
    }

    fun startConnection() {
        val client = billingClient ?: return
        if (client.isReady) {
            queryCatalogAndPurchases()
            return
        }
        try {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryCatalogAndPurchases()
                    } else {
                        Log.i(TAG, "Billing setup indisponível (code=${billingResult.responseCode})")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // enableAutoServiceReconnection() handles reconnecting on next API call
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao conectar ao Google Play Billing: ${e.javaClass.simpleName}")
        }
    }

    fun refreshPurchases(showUserFeedback: Boolean = false) {
        val client = billingClient
        if (client == null) {
            if (showUserFeedback) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "Google Play Billing indisponível neste aparelho.",
                    )
                }
            }
            return
        }
        if (!client.isReady) {
            if (showUserFeedback) {
                _state.update { it.copy(isLoading = true, statusMessage = "Conectando ao Google Play...") }
            }
            startConnection()
            return
        }
        if (showUserFeedback) {
            _state.update { it.copy(isLoading = true, statusMessage = null) }
        }
        queryActivePurchases(showUserFeedback)
    }

    private fun queryCatalogAndPurchases() {
        querySubscriptionProduct()
        queryLifetimeProduct()
        queryActivePurchases(showUserFeedback = false)
    }

    private fun querySubscriptionProduct() {
        val client = billingClient ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_PRO_ANNUAL)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        client.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val product = queryResult.productDetailsList.firstOrNull {
                    it.productId == PRODUCT_ID_PRO_ANNUAL
                }
                annualProductDetails = product
                val formatted = product?.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice
                _state.update { prev ->
                    prev.copy(
                        annualPriceLabel = if (!formatted.isNullOrBlank()) "$formatted / ano" else prev.annualPriceLabel,
                        storeCatalogReady = product != null || lifetimeProductDetails != null,
                    )
                }
            }
        }
    }

    private fun queryLifetimeProduct() {
        val client = billingClient ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_PRO_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        client.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val product = queryResult.productDetailsList.firstOrNull {
                    it.productId == PRODUCT_ID_PRO_LIFETIME
                }
                lifetimeProductDetails = product
                val formatted = product?.oneTimePurchaseOfferDetails?.formattedPrice
                _state.update { prev ->
                    prev.copy(
                        lifetimePriceLabel = if (!formatted.isNullOrBlank()) formatted else prev.lifetimePriceLabel,
                        storeCatalogReady = product != null || annualProductDetails != null,
                    )
                }
            }
        }
    }

    private fun queryActivePurchases(showUserFeedback: Boolean) {
        val client = billingClient ?: return
        var subsChecked = false
        var inAppChecked = false
        var foundAnnual = false
        var foundLifetime = false
        var foundPending = false

        fun finishIfBothChecked() {
            if (!subsChecked || !inAppChecked) return
            verifiedAnnualOwned = foundAnnual
            verifiedLifetimeOwned = foundLifetime
            pendingPurchaseDetected = foundPending
            recomputeEntitlementState(
                feedback = if (showUserFeedback) {
                    when {
                        foundLifetime -> "Acesso Pro Vitalício restaurado com sucesso."
                        foundAnnual -> "Assinatura Pro Anual restaurada com sucesso."
                        foundPending -> "Há um pagamento pendente de confirmação no Google Play."
                        else -> "Nenhuma compra ativa encontrada nesta conta Google Play."
                    }
                } else {
                    null
                }
            )
        }

        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(subsParams) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { purchase ->
                    if (purchase.products.contains(PRODUCT_ID_PRO_ANNUAL)) {
                        when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> {
                                foundAnnual = true
                                acknowledgeIfNeeded(purchase)
                            }
                            Purchase.PurchaseState.PENDING -> foundPending = true
                        }
                    }
                }
            }
            subsChecked = true
            finishIfBothChecked()
        }

        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(inAppParams) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { purchase ->
                    if (purchase.products.contains(PRODUCT_ID_PRO_LIFETIME)) {
                        when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> {
                                foundLifetime = true
                                acknowledgeIfNeeded(purchase)
                            }
                            Purchase.PurchaseState.PENDING -> foundPending = true
                        }
                    }
                }
            }
            inAppChecked = true
            finishIfBothChecked()
        }
    }

    fun launchPurchase(activity: Activity, plan: ProPlanChoice) {
        val client = billingClient
        if (client == null || !client.isReady) {
            startConnection()
            _state.update {
                it.copy(
                    statusMessage = "Google Play ainda não está pronto. Verifique se o app foi instalado pela Play Store e tente novamente."
                )
            }
            return
        }

        val details = when (plan) {
            ProPlanChoice.ANNUAL -> annualProductDetails
            ProPlanChoice.LIFETIME -> lifetimeProductDetails
        }

        if (details == null) {
            queryCatalogAndPurchases()
            _state.update {
                it.copy(
                    statusMessage = "Este plano estará disponível assim que o app e os produtos (${PRODUCT_ID_PRO_ANNUAL} / ${PRODUCT_ID_PRO_LIFETIME}) forem publicados no Google Play Console."
                )
            }
            return
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (plan == ProPlanChoice.ANNUAL) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken.isNullOrBlank()) {
                _state.update {
                    it.copy(statusMessage = "Nenhuma oferta ativa encontrada para o plano anual no Google Play.")
                }
                return
            }
            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()

        val result = client.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update {
                it.copy(statusMessage = "Não foi possível abrir o checkout do Google Play (código ${result.responseCode}).")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val list = purchases.orEmpty()
                list.forEach { purchase ->
                    val isAnnual = purchase.products.contains(PRODUCT_ID_PRO_ANNUAL)
                    val isLifetime = purchase.products.contains(PRODUCT_ID_PRO_LIFETIME)
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            if (isAnnual) verifiedAnnualOwned = true
                            if (isLifetime) verifiedLifetimeOwned = true
                            pendingPurchaseDetected = false
                            acknowledgeIfNeeded(purchase)
                        }
                        Purchase.PurchaseState.PENDING -> {
                            if (isAnnual || isLifetime) {
                                pendingPurchaseDetected = true
                            }
                        }
                    }
                }
                val msg = when {
                    verifiedLifetimeOwned -> "Escuta e Grava Pro Vitalício ativado! Gravação liberada."
                    verifiedAnnualOwned -> "Escuta e Grava Pro Anual ativado! Gravação liberada."
                    pendingPurchaseDetected -> "Pagamento pendente. Assim que o Google Play confirmar, a gravação será liberada automaticamente."
                    else -> null
                }
                recomputeEntitlementState(feedback = msg)
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.update {
                    it.copy(isLoading = false, statusMessage = "Compra cancelada. Você continua no modo teste gratuito.")
                }
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                refreshPurchases(showUserFeedback = true)
            }

            else -> {
                _state.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "Não foi possível concluir a compra (código ${billingResult.responseCode}).",
                    )
                }
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val client = billingClient ?: return
        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(ackParams) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Falha ao confirmar compra (code=${result.responseCode})")
            }
        }
    }

    /**
     * Strictly gated behind BuildConfig.DEBUG so release Play Store AABs cannot toggle Pro without billing.
     */
    fun setDebugProOverride(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        debugOverrideActive = enabled
        recomputeEntitlementState(
            feedback = if (enabled) {
                "Modo Pro simulado (somente versão Debug de desenvolvimento)."
            } else {
                "Simulação desativada — exibindo o Modo Teste gratuito."
            }
        )
    }

    fun clearStatusMessage() {
        _state.update { it.copy(statusMessage = null) }
    }

    private fun recomputeEntitlementState(feedback: String?) {
        val debugUnlocked = BuildConfig.DEBUG && debugOverrideActive
        val unlocked = verifiedLifetimeOwned || verifiedAnnualOwned || debugUnlocked
        val label = when {
            verifiedLifetimeOwned -> "Pro Vitalício"
            verifiedAnnualOwned -> "Pro Anual"
            debugUnlocked -> "Pro (Debug)"
            else -> null
        }
        _state.update { prev ->
            prev.copy(
                isProUnlocked = unlocked,
                activePlanLabel = label,
                isPendingPayment = !unlocked && pendingPurchaseDetected,
                isLoading = false,
                statusMessage = feedback,
                debugProOverrideEnabled = debugUnlocked,
            )
        }
    }

    fun release() {
        try {
            billingClient?.endConnection()
        } catch (_: Exception) {
        }
        billingClient = null
    }

    companion object {
        private const val TAG = "PlayBillingManager"
        const val PRODUCT_ID_PRO_ANNUAL = "escuta_grava_pro_anual"
        const val PRODUCT_ID_PRO_LIFETIME = "escuta_grava_pro_vitalicio"
    }
}
