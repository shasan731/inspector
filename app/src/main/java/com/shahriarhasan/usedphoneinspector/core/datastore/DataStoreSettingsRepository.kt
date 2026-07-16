package com.shahriarhasan.usedphoneinspector.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shahriarhasan.usedphoneinspector.core.model.AppLanguage
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import com.shahriarhasan.usedphoneinspector.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_complete")
        val theme = stringPreferencesKey("theme")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val language = stringPreferencesKey("language")
        val profile = stringPreferencesKey("default_profile")
        val currency = stringPreferencesKey("default_currency")
        val keepAwake = booleanPreferencesKey("keep_screen_awake")
        val failureNotes = booleanPreferencesKey("require_failure_notes")
        val confirmSkip = booleanPreferencesKey("confirm_before_skip")
        val includeSeller = booleanPreferencesKey("include_seller")
        val includeImei = booleanPreferencesKey("include_imei")
        val includePhotos = booleanPreferencesKey("include_photos")
        val imageQuality = intPreferencesKey("pdf_image_quality")
        val footer = stringPreferencesKey("report_footer")
    }

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences ->
            AppSettings(
                onboardingComplete = preferences[Keys.onboarding] ?: false,
                themeMode = preferences[Keys.theme].enumOr(ThemeMode.SYSTEM),
                dynamicColor = preferences[Keys.dynamicColor] ?: true,
                language = preferences[Keys.language].enumOr(AppLanguage.SYSTEM),
                defaultProfile = preferences[Keys.profile].enumOr(InspectionProfile.USED_PHONE),
                defaultCurrency = preferences[Keys.currency] ?: "BDT",
                keepScreenAwake = preferences[Keys.keepAwake] ?: true,
                requireFailureNotes = preferences[Keys.failureNotes] ?: true,
                confirmBeforeSkip = preferences[Keys.confirmSkip] ?: true,
                includeSeller = preferences[Keys.includeSeller] ?: true,
                includeImei = preferences[Keys.includeImei] ?: false,
                includePhotos = preferences[Keys.includePhotos] ?: true,
                pdfImageQuality = preferences[Keys.imageQuality] ?: 75,
                reportFooter = preferences[Keys.footer] ?: "",
            )
        }

    override suspend fun setOnboardingComplete(complete: Boolean) = update(Keys.onboarding, complete)
    override suspend fun setTheme(mode: ThemeMode) = update(Keys.theme, mode.name)
    override suspend fun setDynamicColor(enabled: Boolean) = update(Keys.dynamicColor, enabled)
    override suspend fun setLanguage(language: AppLanguage) = update(Keys.language, language.name)
    override suspend fun setDefaultProfile(profile: InspectionProfile) = update(Keys.profile, profile.name)
    override suspend fun setDefaultCurrency(currency: String) = update(Keys.currency, currency.trim().uppercase().take(8))
    override suspend fun setKeepScreenAwake(enabled: Boolean) = update(Keys.keepAwake, enabled)
    override suspend fun setRequireFailureNotes(enabled: Boolean) = update(Keys.failureNotes, enabled)
    override suspend fun setConfirmBeforeSkip(enabled: Boolean) = update(Keys.confirmSkip, enabled)

    override suspend fun setReportPreferences(includeSeller: Boolean, includeImei: Boolean, includePhotos: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.includeSeller] = includeSeller
            it[Keys.includeImei] = includeImei
            it[Keys.includePhotos] = includePhotos
        }
    }

    override suspend fun setPdfImageQuality(quality: Int) = update(Keys.imageQuality, quality.coerceIn(40, 95))
    override suspend fun setReportFooter(footer: String) = update(Keys.footer, footer.take(500))
    override suspend fun clear() { context.settingsDataStore.edit { it.clear() } }

    private suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private inline fun <reified T : Enum<T>> String?.enumOr(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}

