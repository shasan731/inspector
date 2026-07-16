package com.shahriarhasan.usedphoneinspector.feature

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shahriarhasan.usedphoneinspector.core.billing.BillingConnectionState
import com.shahriarhasan.usedphoneinspector.core.billing.BillingUiState
import com.shahriarhasan.usedphoneinspector.core.database.InspectionStats
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus
import com.shahriarhasan.usedphoneinspector.core.testing.FakeSensorRepository
import com.shahriarhasan.usedphoneinspector.feature.home.HomeScreen
import com.shahriarhasan.usedphoneinspector.feature.home.HomeUiState
import com.shahriarhasan.usedphoneinspector.feature.inspection.InspectionSetupScreen
import com.shahriarhasan.usedphoneinspector.feature.inspection.ManualResultControls
import com.shahriarhasan.usedphoneinspector.feature.inspection.SetupUiState
import com.shahriarhasan.usedphoneinspector.feature.onboarding.OnboardingScreen
import com.shahriarhasan.usedphoneinspector.feature.sensortest.SensorTestScreen
import com.shahriarhasan.usedphoneinspector.feature.upgrade.UpgradeScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeScreen_showsEmptyStateAndNewInspection() {
        compose.setContent {
            MaterialTheme {
                HomeScreen(HomeUiState(stats = InspectionStats(0, null, 0, 0, 0)), {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("New inspection").assertIsDisplayed()
        compose.onNodeWithText("No inspections yet").assertIsDisplayed()
    }

    @Test fun setupScreen_showsPrefilledIdentityAndStartAction() {
        compose.setContent { MaterialTheme { InspectionSetupScreen(SetupUiState()) {} } }
        compose.onNodeWithText("Set up inspection").assertIsDisplayed()
        compose.onNodeWithText("Start inspection").assertIsDisplayed()
    }

    @Test fun onboarding_explainsPrivacy() {
        compose.setContent { MaterialTheme { OnboardingScreen {} } }
        compose.onNodeWithText("Inspect with a clear process").assertIsDisplayed()
    }

    @Test fun unsupportedSensorState_isVisible() {
        compose.setContent { MaterialTheme { SensorTestScreen(FakeSensorRepository()) {} } }
        compose.onNodeWithText("This hardware is not available on the device.").assertIsDisplayed()
    }

    @Test fun manualPassFailSelection_dispatchesEvents() {
        var pass = false
        compose.setContent { MaterialTheme { ManualResultControls({ pass = true }, {}, {}) } }
        compose.onNodeWithText("Mark pass").performClick()
        assertTrue(pass)
    }

    @Test fun upgradeScreen_showsBillingUnavailableState() {
        compose.setContent {
            MaterialTheme {
                UpgradeScreen(BillingUiState(connection = BillingConnectionState.UNAVAILABLE), {}, {})
            }
        }
        compose.onNodeWithText("Google Play Billing is unavailable. Cached entitlement remains active offline.").assertIsDisplayed()
    }
}

