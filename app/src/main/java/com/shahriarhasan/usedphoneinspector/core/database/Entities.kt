package com.shahriarhasan.usedphoneinspector.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shahriarhasan.usedphoneinspector.core.model.ConditionGrade
import com.shahriarhasan.usedphoneinspector.core.model.DeviceCategory
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import com.shahriarhasan.usedphoneinspector.core.model.InspectionStatus
import com.shahriarhasan.usedphoneinspector.core.model.PhotoType
import com.shahriarhasan.usedphoneinspector.core.model.PhysicalCondition
import com.shahriarhasan.usedphoneinspector.core.model.TestCategory
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus

@Entity(
    tableName = "inspections",
    indices = [
        Index("created_at"),
        Index("completed_at"),
        Index("profile"),
        Index("grade"),
        Index("status"),
        Index("brand"),
        Index("model"),
        Index(value = ["report_id"], unique = true),
    ],
)
data class InspectionEntity(
    @PrimaryKey val id: String,
    val profile: InspectionProfile,
    val status: InspectionStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(name = "last_modified_at") val lastModifiedAt: Long?,
    @ColumnInfo(name = "report_id") val reportId: String?,
    val brand: String,
    val model: String,
    @ColumnInfo(name = "device_category") val deviceCategory: DeviceCategory,
    val color: String,
    @ColumnInfo(name = "storage_variant") val storageVariant: String,
    @ColumnInfo(name = "ram_variant") val ramVariant: String,
    @ColumnInfo(name = "asking_price") val askingPrice: String,
    @ColumnInfo(name = "final_price") val finalPrice: String,
    val currency: String,
    @ColumnInfo(name = "purchase_source") val purchaseSource: String,
    val imei1: String,
    val imei2: String,
    @ColumnInfo(name = "serial_number") val serialNumber: String,
    @ColumnInfo(name = "condition_score") val conditionScore: Int?,
    @ColumnInfo(name = "coverage_percent") val coveragePercent: Int?,
    val grade: ConditionGrade?,
    val notes: String,
    @ColumnInfo(name = "score_config_version") val scoreConfigVersion: Int,
    @ColumnInfo(name = "current_test_id") val currentTestId: String?,
    @ColumnInfo(name = "completion_snapshot_json") val completionSnapshotJson: String?,
)

@Entity(
    tableName = "test_results",
    foreignKeys = [ForeignKey(
        entity = InspectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["inspection_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("inspection_id"), Index(value = ["inspection_id", "test_id"], unique = true)],
)
data class TestResultEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "inspection_id") val inspectionId: String,
    @ColumnInfo(name = "test_id") val testId: String,
    val category: TestCategory,
    val status: TestStatus,
    val required: Boolean,
    val notes: String,
    @ColumnInfo(name = "issues_json") val issuesJson: String,
    @ColumnInfo(name = "readings_json") val readingsJson: String,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "physical_checks",
    foreignKeys = [ForeignKey(
        entity = InspectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["inspection_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("inspection_id"), Index(value = ["inspection_id", "item_key"], unique = true)],
)
data class PhysicalCheckEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "inspection_id") val inspectionId: String,
    @ColumnInfo(name = "item_key") val itemKey: String,
    @ColumnInfo(name = "category_key") val categoryKey: String,
    val condition: PhysicalCondition,
    val notes: String,
)

@Entity(
    tableName = "sellers",
    foreignKeys = [ForeignKey(
        entity = InspectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["inspection_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("inspection_id")],
)
data class SellerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "inspection_id") val inspectionId: String,
    val name: String,
    val phone: String,
    @ColumnInfo(name = "alternate_phone") val alternatePhone: String,
    val email: String,
    @ColumnInfo(name = "business_name") val businessName: String,
    val location: String,
    val address: String,
    @ColumnInfo(name = "national_id_reference") val nationalIdReference: String,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    val warranty: String,
    @ColumnInfo(name = "purchase_date") val purchaseDate: String,
    @ColumnInfo(name = "seller_notes") val sellerNotes: String,
    @ColumnInfo(name = "buyer_notes") val buyerNotes: String,
    @ColumnInfo(name = "repair_details_json") val repairDetailsJson: String,
)

@Entity(
    tableName = "inspection_photos",
    foreignKeys = [ForeignKey(
        entity = InspectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["inspection_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("inspection_id"), Index(value = ["inspection_id", "sort_order"])],
)
data class InspectionPhotoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "inspection_id") val inspectionId: String,
    val type: PhotoType,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String,
    val description: String,
    @ColumnInfo(name = "exclude_from_report") val excludeFromReport: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "device_snapshots",
    foreignKeys = [ForeignKey(
        entity = InspectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["inspection_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["inspection_id"], unique = true)],
)
data class DeviceSnapshotEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "inspection_id") val inspectionId: String,
    @ColumnInfo(name = "values_json") val valuesJson: String,
    @ColumnInfo(name = "captured_at") val capturedAt: Long,
)

@Entity(
    tableName = "reports",
    foreignKeys = [ForeignKey(
        entity = InspectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["inspection_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("inspection_id")],
)
data class ReportEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "inspection_id") val inspectionId: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    val language: String,
    @ColumnInfo(name = "is_full_report") val isFullReport: Boolean,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
)

@Entity(tableName = "custom_checklist_items", indices = [Index("profile")])
data class CustomChecklistItemEntity(
    @PrimaryKey val id: String,
    val profile: InspectionProfile,
    @ColumnInfo(name = "category_key") val categoryKey: String,
    val label: String,
    val enabled: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(tableName = "branding_profiles")
data class BrandingProfileEntity(
    @PrimaryKey val id: String = "default",
    @ColumnInfo(name = "business_name") val businessName: String,
    @ColumnInfo(name = "logo_path") val logoPath: String,
    val address: String,
    val phone: String,
    val email: String,
    val website: String,
    @ColumnInfo(name = "report_title") val reportTitle: String,
    @ColumnInfo(name = "footer_text") val footerText: String,
)

