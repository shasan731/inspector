package com.shahriarhasan.usedphoneinspector.feature.home

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahriarhasan.usedphoneinspector.core.billing.BillingRepository
import com.shahriarhasan.usedphoneinspector.core.billing.BillingUiState
import com.shahriarhasan.usedphoneinspector.core.database.InspectionEntity
import com.shahriarhasan.usedphoneinspector.core.database.InspectionRepository
import com.shahriarhasan.usedphoneinspector.core.database.InspectionStats
import com.shahriarhasan.usedphoneinspector.core.database.InspectionWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val resume: InspectionWithDetails? = null,
    val recent: List<InspectionEntity> = emptyList(),
    val stats: InspectionStats = InspectionStats(0, null, 0, 0, 0),
    val billing: BillingUiState = BillingUiState(),
    val deviceModel: String = "",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: InspectionRepository,
    billingRepository: BillingRepository,
) : ViewModel() {
    val state = combine(
        repository.observeResumable(),
        repository.observeRecent(),
        repository.observeStats(),
        billingRepository.state,
    ) { resume, recent, stats, billing ->
        HomeUiState(resume, recent, stats, billing, "${Build.MANUFACTURER} ${Build.MODEL}".trim())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

