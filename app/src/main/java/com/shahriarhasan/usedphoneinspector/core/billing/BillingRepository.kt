package com.shahriarhasan.usedphoneinspector.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

enum class BillingConnectionState { CONNECTING, READY, UNAVAILABLE }
enum class PurchaseState { IDLE, LAUNCHING, PENDING, PURCHASED, CANCELLED, ERROR }

data class BillingUiState(
    val connection: BillingConnectionState = BillingConnectionState.CONNECTING,
    val purchaseState: PurchaseState = PurchaseState.IDLE,
    val isPro: Boolean = false,
    val localizedPrice: String? = null,
    val diagnosticMessage: String? = null,
)

interface BillingRepository {
    val state: StateFlow<BillingUiState>
    fun connect()
    fun launchPurchase(activity: Activity)
    fun restorePurchases()
    fun close()
}

object EntitlementPolicy {
    const val FREE_COMPLETED_INSPECTION_LIMIT = 3
    fun canComplete(completedCount: Int, isPro: Boolean): Boolean =
        isPro || completedCount < FREE_COMPLETED_INSPECTION_LIMIT
    fun reportPhotoLimit(isPro: Boolean): Int = if (isPro) Int.MAX_VALUE else 2
}

