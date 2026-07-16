package com.shahriarhasan.usedphoneinspector.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class InspectionStats(
    val total: Int,
    val averageScore: Double?,
    val passed: Int,
    val warnings: Int,
    val failed: Int,
)

@Dao
interface InspectionDao {
    @Upsert
    suspend fun upsertInspection(inspection: InspectionEntity)

    @Upsert
    suspend fun upsertTestResults(results: List<TestResultEntity>)

    @Upsert
    suspend fun upsertTestResult(result: TestResultEntity)

    @Upsert
    suspend fun upsertPhysicalChecks(checks: List<PhysicalCheckEntity>)

    @Upsert
    suspend fun upsertSeller(seller: SellerEntity)

    @Upsert
    suspend fun upsertPhoto(photo: InspectionPhotoEntity)

    @Upsert
    suspend fun upsertSnapshot(snapshot: DeviceSnapshotEntity)

    @Upsert
    suspend fun upsertReport(report: ReportEntity)

    @Upsert
    suspend fun upsertBranding(branding: BrandingProfileEntity)

    @Query("SELECT * FROM inspections WHERE id = :id")
    suspend fun getInspection(id: String): InspectionEntity?

    @Transaction
    @Query("SELECT * FROM inspections WHERE id = :id")
    suspend fun getInspectionWithDetails(id: String): InspectionWithDetails?

    @Transaction
    @Query("SELECT * FROM inspections WHERE id = :id")
    fun observeInspection(id: String): Flow<InspectionWithDetails?>

    @Transaction
    @Query("SELECT * FROM inspections WHERE status != 'COMPLETED' ORDER BY updated_at DESC LIMIT 1")
    fun observeResumable(): Flow<InspectionWithDetails?>

    @Query("SELECT * FROM inspections ORDER BY updated_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<InspectionEntity>>

    @Query(
        """
        SELECT COUNT(*) AS total,
               AVG(condition_score) AS averageScore,
               COALESCE(SUM(CASE WHEN condition_score >= 75 AND coverage_percent >= 60 THEN 1 ELSE 0 END), 0) AS passed,
               COALESCE(SUM(CASE WHEN condition_score >= 60 AND condition_score < 75 AND coverage_percent >= 60 THEN 1 ELSE 0 END), 0) AS warnings,
               COALESCE(SUM(CASE WHEN condition_score < 60 AND coverage_percent >= 60 THEN 1 ELSE 0 END), 0) AS failed
        FROM inspections WHERE status = 'COMPLETED'
        """,
    )
    fun observeStats(): Flow<InspectionStats>

    @Transaction
    @Query("SELECT * FROM inspections ORDER BY created_at ASC")
    suspend fun getAllWithDetails(): List<InspectionWithDetails>

    @Query("SELECT * FROM branding_profiles WHERE id = 'default' LIMIT 1")
    fun observeBranding(): Flow<BrandingProfileEntity?>

    @Transaction
    @Query(
        """
        SELECT DISTINCT i.* FROM inspections i
        LEFT JOIN sellers s ON s.inspection_id = i.id
        WHERE (:query = '' OR i.brand LIKE '%' || :query || '%' OR i.model LIKE '%' || :query || '%'
            OR i.imei1 LIKE '%' || :query || '%' OR i.imei2 LIKE '%' || :query || '%'
            OR i.report_id LIKE '%' || :query || '%' OR s.name LIKE '%' || :query || '%')
          AND (:profile IS NULL OR i.profile = :profile)
          AND (:grade IS NULL OR i.grade = :grade)
          AND (:status IS NULL OR i.status = :status)
          AND (:startDate IS NULL OR i.created_at >= :startDate)
          AND (:endDate IS NULL OR i.created_at <= :endDate)
        ORDER BY
          CASE WHEN :sort = 'NEWEST' THEN i.created_at END DESC,
          CASE WHEN :sort = 'OLDEST' THEN i.created_at END ASC,
          CASE WHEN :sort = 'HIGHEST_SCORE' THEN i.condition_score END DESC,
          CASE WHEN :sort = 'LOWEST_SCORE' THEN i.condition_score END ASC
        """,
    )
    fun search(
        query: String,
        profile: String?,
        grade: String?,
        status: String?,
        startDate: Long?,
        endDate: Long?,
        sort: String,
    ): Flow<List<InspectionWithDetails>>

    @Query("SELECT COUNT(*) FROM inspections WHERE status = 'COMPLETED'")
    suspend fun completedCount(): Int

    @Delete
    suspend fun deleteInspection(inspection: InspectionEntity)

    @Delete
    suspend fun deletePhoto(photo: InspectionPhotoEntity)

    @Query("DELETE FROM inspections")
    suspend fun deleteAll()
}
