package com.shahriarhasan.usedphoneinspector.core.database

import com.shahriarhasan.usedphoneinspector.core.model.HistoryFilter
import com.shahriarhasan.usedphoneinspector.core.model.InspectionDraft
import com.shahriarhasan.usedphoneinspector.core.model.SellerDetails
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus
import kotlinx.coroutines.flow.Flow

interface InspectionRepository {
    fun observeInspection(id: String): Flow<InspectionWithDetails?>
    fun observeResumable(): Flow<InspectionWithDetails?>
    fun observeRecent(limit: Int = 5): Flow<List<InspectionEntity>>
    fun observeStats(): Flow<InspectionStats>
    fun search(filter: HistoryFilter): Flow<List<InspectionWithDetails>>
    suspend fun create(draft: InspectionDraft): String
    suspend fun updateDraft(id: String, draft: InspectionDraft)
    suspend fun updateCurrentTest(id: String, testId: String)
    suspend fun saveTestResult(
        inspectionId: String,
        testId: String,
        status: TestStatus,
        notes: String,
        issuesJson: String = "[]",
        readingsJson: String = "{}",
        durationMillis: Long = 0,
    )
    suspend fun saveSeller(inspectionId: String, seller: SellerDetails)
    suspend fun savePhysicalChecks(checks: List<PhysicalCheckEntity>)
    suspend fun complete(id: String): InspectionEntity
    suspend fun reopen(id: String)
    suspend fun duplicate(id: String): String
    suspend fun delete(id: String)
    suspend fun deleteAll()
    suspend fun completedCount(): Int
    suspend fun exportInspection(id: String): String
    suspend fun importInspection(json: String): String
}

