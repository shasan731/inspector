package com.shahriarhasan.usedphoneinspector.core.model

import androidx.annotation.StringRes
import com.shahriarhasan.usedphoneinspector.R

enum class TestKind {
    DEVICE_INFO,
    DISPLAY,
    MULTITOUCH,
    SPEAKER,
    MICROPHONE,
    CAMERA,
    VIBRATION,
    SENSORS,
    CHARGING,
    BATTERY,
    WIFI,
    BLUETOOTH,
    MOBILE,
    PHYSICAL,
    SELLER,
}

data class TestDefinition(
    val id: String,
    @StringRes val titleRes: Int,
    val category: TestCategory,
    val kind: TestKind,
    val optional: Boolean = false,
    val phoneOnly: Boolean = false,
)

object InspectionProfiles {
    const val SCORE_CONFIG_VERSION = 1

    val allTests = listOf(
        TestDefinition("device_information", R.string.test_device_information, TestCategory.IDENTITY, TestKind.DEVICE_INFO),
        TestDefinition("display_colour", R.string.test_display_colour, TestCategory.DISPLAY, TestKind.DISPLAY),
        TestDefinition("multitouch", R.string.test_multitouch, TestCategory.DISPLAY, TestKind.MULTITOUCH),
        TestDefinition("main_speaker", R.string.test_main_speaker, TestCategory.AUDIO, TestKind.SPEAKER),
        TestDefinition("microphone", R.string.test_microphone, TestCategory.AUDIO, TestKind.MICROPHONE),
        TestDefinition("cameras_flash", R.string.test_camera_flash, TestCategory.CAMERA, TestKind.CAMERA),
        TestDefinition("vibration", R.string.test_vibration, TestCategory.SENSORS, TestKind.VIBRATION),
        TestDefinition("sensors", R.string.test_sensors, TestCategory.SENSORS, TestKind.SENSORS),
        TestDefinition("charging", R.string.test_charging, TestCategory.BATTERY, TestKind.CHARGING),
        TestDefinition("battery_information", R.string.test_battery_information, TestCategory.BATTERY, TestKind.BATTERY),
        TestDefinition("wifi", R.string.test_wifi, TestCategory.CONNECTIVITY, TestKind.WIFI),
        TestDefinition("bluetooth", R.string.test_bluetooth, TestCategory.CONNECTIVITY, TestKind.BLUETOOTH, optional = true),
        TestDefinition("mobile_network", R.string.test_mobile_network, TestCategory.CONNECTIVITY, TestKind.MOBILE, phoneOnly = true),
        TestDefinition("physical_checklist", R.string.test_physical, TestCategory.PHYSICAL, TestKind.PHYSICAL),
        TestDefinition("seller_information", R.string.test_seller, TestCategory.SELLER, TestKind.SELLER, optional = true),
    )

    fun testsFor(profile: InspectionProfile, hasTelephony: Boolean = true): List<TestDefinition> {
        val base = allTests.filterNot { it.phoneOnly && (!hasTelephony || profile == InspectionProfile.USED_TABLET) }
        return when (profile) {
            InspectionProfile.USED_PHONE,
            InspectionProfile.USED_TABLET,
            InspectionProfile.REFURBISHED_DEVICE -> base
            InspectionProfile.REPAIR_SHOP_INTAKE -> base.filterNot { it.id == "seller_information" } +
                allTests.first { it.id == "seller_information" }
            InspectionProfile.TRADE_IN_ASSESSMENT -> base
        }
    }

    fun weights(profile: InspectionProfile): Map<TestCategory, Int> = when (profile) {
        InspectionProfile.USED_TABLET -> mapOf(
            TestCategory.DISPLAY to 25,
            TestCategory.AUDIO to 10,
            TestCategory.CAMERA to 10,
            TestCategory.SENSORS to 10,
            TestCategory.BATTERY to 15,
            TestCategory.CONNECTIVITY to 10,
            TestCategory.PHYSICAL to 20,
        )
        InspectionProfile.REPAIR_SHOP_INTAKE -> mapOf(
            TestCategory.DISPLAY to 15,
            TestCategory.AUDIO to 10,
            TestCategory.CAMERA to 10,
            TestCategory.SENSORS to 10,
            TestCategory.BATTERY to 15,
            TestCategory.CONNECTIVITY to 10,
            TestCategory.PHYSICAL to 30,
        )
        else -> mapOf(
            TestCategory.DISPLAY to 20,
            TestCategory.AUDIO to 10,
            TestCategory.CAMERA to 15,
            TestCategory.SENSORS to 10,
            TestCategory.BATTERY to 15,
            TestCategory.CONNECTIVITY to 10,
            TestCategory.PHYSICAL to 20,
        )
    }
}

