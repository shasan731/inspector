package com.shahriarhasan.usedphoneinspector.feature.upgrade

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.shahriarhasan.usedphoneinspector.core.billing.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    val billingRepository: BillingRepository,
) : ViewModel() {
    fun buy(activity: Activity) = billingRepository.launchPurchase(activity)
    fun restore() = billingRepository.restorePurchases()
}

