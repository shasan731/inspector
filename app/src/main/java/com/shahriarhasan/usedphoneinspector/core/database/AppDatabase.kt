package com.shahriarhasan.usedphoneinspector.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        InspectionEntity::class,
        TestResultEntity::class,
        PhysicalCheckEntity::class,
        SellerEntity::class,
        InspectionPhotoEntity::class,
        DeviceSnapshotEntity::class,
        ReportEntity::class,
        CustomChecklistItemEntity::class,
        BrandingProfileEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_checklist_items` (
                        `id` TEXT NOT NULL,
                        `profile` TEXT NOT NULL,
                        `category_key` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_checklist_items_profile` ON `custom_checklist_items` (`profile`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `branding_profiles` (
                        `id` TEXT NOT NULL,
                        `business_name` TEXT NOT NULL,
                        `logo_path` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `website` TEXT NOT NULL,
                        `report_title` TEXT NOT NULL,
                        `footer_text` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}

