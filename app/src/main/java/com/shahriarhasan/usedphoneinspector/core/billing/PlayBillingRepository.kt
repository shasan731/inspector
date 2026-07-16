package com.shahriarhasan.usedphoneinspector.core.billing

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
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
import com.shahriarhasan.usedphoneinspector.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.entitlementDataStore by preferencesDataStore(name = "entitlement")

class PlayBillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : BillingRepository, PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(BillingUiState())
    override val state: StateFlow<BillingUiState> = mutableState
    private var productDetails: ProductDetails? = null
    private var reconnectAttempted = false

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    init {
        scope.launch {
            val cached = context.entitlementDataStore.data.first()[PRO_KEY] ?: false
            mutableState.value = mutableState.value.copy(isPro = cached)
            connect()
        }
    }

    override fun connect() {
        if (billingClient.isReady) {
            queryProductsAndPurchases()
            return
        }
        mutableState.value = mutableState.value.copy(connection = BillingConnectionState.CONNECTING)
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempted = false
                    mutableState.value = mutableState.value.copy(connection = BillingConnectionState.READY)
                    queryProductsAndPurchases()
                } else {
                    unavailable(result)
                }
            }

            override fun onBillingServiceDisconnected() {
                mutableState.value = mutableState.value.copy(connection = BillingConnectionState.UNAVAILABLE)
                if (!reconnectAttempted) {
                    reconnectAttempted = true
                    connect()
                }
            }
        })
    }

    override fun launchPurchase(activity: Activity) {
        val details = productDetails ?: run {
            mutableState.value = mutableState.value.copy(
                purchaseState = PurchaseState.ERROR,
                diagnosticMessage = "Product details unavailable",
            )
            queryProductsAndPurchases()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()
        mutableState.value = mutableState.value.copy(purchaseState = PurchaseState.LAUNCHING)
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) unavailable(result)
    }

    override fun restorePurchases() = queryPurchases()

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach(::processPurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                mutableState.value = mutableState.value.copy(purchaseState = PurchaseState.CANCELLED)
            }
            else -> unavailable(result)
        }
    }

    private fun queryProductsAndPurchases() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(BuildConfig.PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        ) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = queryResult.productDetailsList.firstOrNull()
                mutableState.value = mutableState.value.copy(
                    localizedPrice = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice,
                    diagnosticMessage = if (productDetails == null) "lifetime_pro is not configured for this build" else null,
                )
            } else {
                unavailable(result)
            }
        }
        queryPurchases()
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) {
            connect()
            return
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val proPurchase = purchases.firstOrNull { BuildConfig.PRO_PRODUCT_ID in it.products }
                if (proPurchase == null) updateEntitlement(false, PurchaseState.IDLE) else processPurchase(proPurchase)
            } else {
                unavailable(result)
            }
        }
    }

    private fun processPurchase(purchase: Purchase) {
        if (BuildConfig.PRO_PRODUCT_ID !in purchase.products) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> updateEntitlement(false, PurchaseState.PENDING)
            Purchase.PurchaseState.PURCHASED -> {
                updateEntitlement(true, PurchaseState.PURCHASED)
                if (!purchase.isAcknowledged) {
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(params) { result ->
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) unavailable(result)
                    }
                }
            }
            else -> updateEntitlement(false, PurchaseState.IDLE)
        }
    }

    private fun updateEntitlement(isPro: Boolean, purchaseState: PurchaseState) {
        mutableState.value = mutableState.value.copy(isPro = isPro, purchaseState = purchaseState)
        scope.launch { context.entitlementDataStore.edit { it[PRO_KEY] = isPro } }
    }

    private fun unavailable(result: BillingResult) {
        mutableState.value = mutableState.value.copy(
            connection = BillingConnectionState.UNAVAILABLE,
            purchaseState = PurchaseState.ERROR,
            diagnosticMessage = result.debugMessage.take(200),
        )
    }

    override fun close() {
        if (billingClient.isReady) billingClient.endConnection()
    }

    private companion object {
        val PRO_KEY = booleanPreferencesKey("lifetime_pro_entitled")
    }
}

