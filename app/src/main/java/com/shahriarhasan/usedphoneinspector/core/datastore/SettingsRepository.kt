package com.shahriarhasan.usedphoneinspector.core.datastore

import com.shahriarhasan.usedphoneinspector.core.model.AppLanguage
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import com.shahriarhasan.usedphoneinspector.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val defaultProfile: InspectionProfile = InspectionProfile.USED_PHONE,
    val defaultCurrency: String = "BDT",
    val keepScreenAwake: Boolean = true,
    val requireFailureNotes: Boolean = true,
    val confirmBeforeSkip: Boolean = true,
    val includeSeller: Boolean = true,
    val includeImei: Boolean = false,
    val includePhotos: Boolean = true,
    val pdfImageQuality: Int = 75,
    val reportFooter: String = "",
    val dateFormat: String = "yyyy-MM-dd",
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setTheme(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setDefaultProfile(profile: InspectionProfile)
    suspend fun setDefaultCurrency(currency: String)
    suspend fun setKeepScreenAwake(enabled: Boolean)
    suspend fun setRequireFailureNotes(enabled: Boolean)
    suspend fun setConfirmBeforeSkip(enabled: Boolean)
    suspend fun setReportPreferences(includeSeller: Boolean, includeImei: Boolean, includePhotos: Boolean)
    suspend fun setPdfImageQuality(quality: Int)
    suspend fun setReportFooter(footer: String)
    suspend fun clear()
}

