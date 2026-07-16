package com.shahriarhasan.usedphoneinspector.core.billing

import android.app.Activity
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeBillingRepository @Inject constructor() : BillingRepository {
    private val mutableState = MutableStateFlow(
        BillingUiState(
            connection = BillingConnectionState.READY,
            localizedPrice = null,
            diagnosticMessage = "Debug billing simulator",
        ),
    )
    override val state: StateFlow<BillingUiState> = mutableState
    override fun connect() = Unit
    override fun launchPurchase(activity: Activity) {
        mutableState.value = mutableState.value.copy(isPro = true, purchaseState = PurchaseState.PURCHASED)
    }
    override fun restorePurchases() = Unit
    override fun close() = Unit
}

