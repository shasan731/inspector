package com.shahriarhasan.usedphoneinspector.core.model

import androidx.annotation.StringRes
import com.shahriarhasan.usedphoneinspector.R
import kotlinx.serialization.Serializable

@Serializable
enum class InspectionProfile(@StringRes val labelRes: Int) {
    USED_PHONE(R.string.profile_used_phone),
    USED_TABLET(R.string.profile_used_tablet),
    REFURBISHED_DEVICE(R.string.profile_refurbished),
    REPAIR_SHOP_INTAKE(R.string.profile_repair_intake),
    TRADE_IN_ASSESSMENT(R.string.profile_trade_in),
}

@Serializable
enum class InspectionStatus {
    DRAFT,
    IN_PROGRESS,
    COMPLETED,
}

@Serializable
enum class TestStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PASS,
    WARNING,
    FAIL,
    SKIPPED,
    UNSUPPORTED,
    PERMISSION_DENIED;

    val isScored: Boolean get() = this == PASS || this == WARNING || this == FAIL
    val isTerminal: Boolean get() = this !in setOf(NOT_STARTED, IN_PROGRESS)
}

@Serializable
enum class TestCategory(@StringRes val labelRes: Int) {
    DISPLAY(R.string.category_display),
    AUDIO(R.string.category_audio),
    CAMERA(R.string.category_camera),
    SENSORS(R.string.category_sensors),
    BATTERY(R.string.category_battery),
    CONNECTIVITY(R.string.category_connectivity),
    PHYSICAL(R.string.category_physical),
    IDENTITY(R.string.category_identity),
    SELLER(R.string.category_seller),
}

@Serializable
enum class ConditionGrade(@StringRes val labelRes: Int) {
    EXCELLENT(R.string.grade_excellent),
    GOOD(R.string.grade_good),
    FAIR(R.string.grade_fair),
    POOR(R.string.grade_poor),
    INCOMPLETE(R.string.grade_incomplete),
}

@Serializable
enum class DeviceCategory(@StringRes val labelRes: Int) {
    PHONE(R.string.device_phone),
    TABLET(R.string.device_tablet),
    FOLDABLE(R.string.device_foldable),
    OTHER(R.string.device_other),
}

@Serializable
enum class PhysicalCondition {
    GOOD,
    MINOR_ISSUE,
    MAJOR_ISSUE,
    NOT_CHECKED,
    NOT_APPLICABLE,
}

@Serializable
enum class PhotoType {
    FRONT,
    BACK,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    IMEI_LABEL,
    RECEIPT,
    DAMAGE,
    CAMERA_SAMPLE,
    OTHER,
}

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class AppLanguage { SYSTEM, ENGLISH, BANGLA }

@Serializable
enum class HistorySort { NEWEST, OLDEST, HIGHEST_SCORE, LOWEST_SCORE }

@Serializable
data class InspectionDraft(
    val profile: InspectionProfile = InspectionProfile.USED_PHONE,
    val deviceCategory: DeviceCategory = DeviceCategory.PHONE,
    val brand: String = "",
    val model: String = "",
    val color: String = "",
    val storageVariant: String = "",
    val ramVariant: String = "",
    val askingPrice: String = "",
    val finalPrice: String = "",
    val currency: String = "BDT",
    val purchaseSource: String = "",
    val serialNumber: String = "",
    val imei1: String = "",
    val imei2: String = "",
    val notes: String = "",
)

@Serializable
data class SellerDetails(
    val name: String = "",
    val phone: String = "",
    val alternatePhone: String = "",
    val email: String = "",
    val businessName: String = "",
    val location: String = "",
    val address: String = "",
    val nationalIdReference: String = "",
    val paymentMethod: String = "",
    val warranty: String = "",
    val purchaseDate: String = "",
    val sellerNotes: String = "",
    val buyerNotes: String = "",
    val repairDetailsJson: String = "{}",
)

data class ScoreResult(
    val score: Int,
    val coveragePercent: Int,
    val grade: ConditionGrade,
    val categoryScores: Map<TestCategory, Int>,
    val completedCount: Int,
    val totalRequiredCount: Int,
)

data class HistoryFilter(
    val query: String = "",
    val profile: InspectionProfile? = null,
    val grade: ConditionGrade? = null,
    val status: InspectionStatus? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val sort: HistorySort = HistorySort.NEWEST,
)

