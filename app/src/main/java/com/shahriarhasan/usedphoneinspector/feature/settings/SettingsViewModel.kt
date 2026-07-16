package com.shahriarhasan.usedphoneinspector.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahriarhasan.usedphoneinspector.core.billing.BillingRepository
import com.shahriarhasan.usedphoneinspector.core.billing.BillingUiState
import com.shahriarhasan.usedphoneinspector.core.database.BrandingProfileEntity
import com.shahriarhasan.usedphoneinspector.core.database.InspectionRepository
import com.shahriarhasan.usedphoneinspector.core.datastore.AppSettings
import com.shahriarhasan.usedphoneinspector.core.datastore.BackupRepository
import com.shahriarhasan.usedphoneinspector.core.datastore.BrandingRepository
import com.shahriarhasan.usedphoneinspector.core.datastore.SettingsRepository
import com.shahriarhasan.usedphoneinspector.core.model.AppLanguage
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import com.shahriarhasan.usedphoneinspector.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val billing: BillingUiState = BillingUiState(),
    val branding: BrandingProfileEntity = BrandingProfileEntity(
        businessName = "", logoPath = "", address = "", phone = "", email = "", website = "", reportTitle = "", footerText = "",
    ),
    val operationMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val brandingRepository: BrandingRepository,
    private val backupRepository: BackupRepository,
    private val inspectionRepository: InspectionRepository,
    val billingRepository: BillingRepository,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        billingRepository.state,
        brandingRepository.branding,
    ) { settings, billing, branding -> SettingsUiState(settings, billing, branding) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(value: ThemeMode) = launch { settingsRepository.setTheme(value) }
    fun setDynamicColor(value: Boolean) = launch { settingsRepository.setDynamicColor(value) }
    fun setLanguage(value: AppLanguage) = launch { settingsRepository.setLanguage(value) }
    fun setProfile(value: InspectionProfile) = launch { settingsRepository.setDefaultProfile(value) }
    fun setCurrency(value: String) = launch { settingsRepository.setDefaultCurrency(value) }
    fun setKeepAwake(value: Boolean) = launch { settingsRepository.setKeepScreenAwake(value) }
    fun setFailureNotes(value: Boolean) = launch { settingsRepository.setRequireFailureNotes(value) }
    fun setConfirmSkip(value: Boolean) = launch { settingsRepository.setConfirmBeforeSkip(value) }
    fun setReportPreferences(seller: Boolean, imei: Boolean, photos: Boolean) =
        launch { settingsRepository.setReportPreferences(seller, imei, photos) }
    fun setImageQuality(value: Int) = launch { settingsRepository.setPdfImageQuality(value) }
    fun setFooter(value: String) = launch { settingsRepository.setReportFooter(value) }
    fun updateBranding(value: BrandingProfileEntity) = launch { brandingRepository.update(value) }
    fun updateLogo(uri: Uri) = launch {
        val path = brandingRepository.updateLogo(uri)
        brandingRepository.update(state.value.branding.copy(logoPath = path))
    }

    fun exportBackup(uri: Uri) = launch {
        val content = backupRepository.exportAll()
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "w").use { output ->
                requireNotNull(output)
                output.writer(Charsets.UTF_8).use { it.write(content) }
            }
        }
    }

    fun importBackup(uri: Uri) = launch {
        val content = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input)
                input.reader(Charsets.UTF_8).use { it.readText() }
            }
        }
        backupRepository.importAll(content)
    }

    fun deleteTemporaryFiles() = launch {
        withContext(Dispatchers.IO) {
            listOf("audio", "camera", "reports").forEach { File(context.cacheDir, it).deleteRecursively() }
        }
    }

    fun deleteAllData() = launch { inspectionRepository.deleteAll() }
    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { runCatching { block() } } }
}

