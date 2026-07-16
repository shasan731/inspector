package com.shahriarhasan.usedphoneinspector.feature.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahriarhasan.usedphoneinspector.core.billing.BillingRepository
import com.shahriarhasan.usedphoneinspector.core.billing.EntitlementPolicy
import com.shahriarhasan.usedphoneinspector.core.database.InspectionRepository
import com.shahriarhasan.usedphoneinspector.core.database.InspectionWithDetails
import com.shahriarhasan.usedphoneinspector.core.datastore.AppSettings
import com.shahriarhasan.usedphoneinspector.core.datastore.SettingsRepository
import com.shahriarhasan.usedphoneinspector.core.model.AppLanguage
import com.shahriarhasan.usedphoneinspector.core.model.InspectionStatus
import com.shahriarhasan.usedphoneinspector.core.model.ScoreEngine
import com.shahriarhasan.usedphoneinspector.core.model.ScoreResult
import com.shahriarhasan.usedphoneinspector.core.model.ScorableResult
import com.shahriarhasan.usedphoneinspector.core.model.TestCategory
import com.shahriarhasan.usedphoneinspector.core.reporting.GeneratedReport
import com.shahriarhasan.usedphoneinspector.core.reporting.PdfReportGenerator
import com.shahriarhasan.usedphoneinspector.core.reporting.ReportActions
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewUiState(
    val details: InspectionWithDetails? = null,
    val score: ScoreResult? = null,
    val settings: AppSettings = AppSettings(),
    val isPro: Boolean = false,
    val report: GeneratedReport? = null,
    val completionLimitReached: Boolean = false,
    val reportError: Boolean = false,
    val busy: Boolean = false,
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: InspectionRepository,
    private val reportGenerator: PdfReportGenerator,
    val reportActions: ReportActions,
    settingsRepository: SettingsRepository,
    billingRepository: BillingRepository,
) : ViewModel() {
    private val inspectionId: String = requireNotNull(savedStateHandle["inspectionId"])
    private val transient = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = combine(
        repository.observeInspection(inspectionId),
        settingsRepository.settings,
        billingRepository.state,
        transient,
    ) { details, settings, billing, local ->
        local.copy(
            details = details,
            settings = settings,
            isPro = billing.isPro,
            score = details?.calculateScore(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    fun complete() {
        viewModelScope.launch {
            val completed = repository.completedCount()
            if (!EntitlementPolicy.canComplete(completed, state.value.isPro)) {
                transient.value = transient.value.copy(completionLimitReached = true)
                return@launch
            }
            transient.value = transient.value.copy(busy = true)
            runCatching { repository.complete(inspectionId) }
                .onFailure { transient.value = transient.value.copy(reportError = true) }
            transient.value = transient.value.copy(busy = false)
        }
    }

    fun reopen() { viewModelScope.launch { repository.reopen(inspectionId) } }

    fun generateReport() {
        val details = state.value.details ?: return
        viewModelScope.launch {
            transient.value = transient.value.copy(busy = true, reportError = false)
            runCatching {
                reportGenerator.generate(
                    details = details,
                    isPro = state.value.isPro,
                    settings = state.value.settings,
                    localeTag = when (state.value.settings.language) {
                        AppLanguage.SYSTEM -> Locale.getDefault().toLanguageTag()
                        AppLanguage.ENGLISH -> "en"
                        AppLanguage.BANGLA -> "bn"
                    },
                )
            }.onSuccess { transient.value = transient.value.copy(report = it) }
                .onFailure { transient.value = transient.value.copy(reportError = true) }
            transient.value = transient.value.copy(busy = false)
        }
    }

    fun dismissLimit() { transient.value = transient.value.copy(completionLimitReached = false) }

    private fun InspectionWithDetails.calculateScore(): ScoreResult = ScoreEngine.calculate(
        inspection.profile,
        testResults.filter { it.category !in setOf(TestCategory.IDENTITY, TestCategory.SELLER) }
            .map { ScorableResult(it.category, it.status, it.required) },
    )
}

