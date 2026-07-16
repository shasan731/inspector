package com.shahriarhasan.usedphoneinspector.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import com.shahriarhasan.usedphoneinspector.core.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataStoreSettingsRepositoryTest {
    @Test fun settingsPersistAndAreExposedAsFlow() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = DataStoreSettingsRepository(context)
        repository.clear()
        repository.setOnboardingComplete(true)
        repository.setTheme(ThemeMode.DARK)
        repository.setDefaultProfile(InspectionProfile.USED_TABLET)
        val settings = repository.settings.first()
        assertTrue(settings.onboardingComplete)
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(InspectionProfile.USED_TABLET, settings.defaultProfile)
        repository.clear()
    }
}

