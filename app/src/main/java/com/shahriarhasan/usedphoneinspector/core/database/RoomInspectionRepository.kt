package com.shahriarhasan.usedphoneinspector.core.database

import androidx.room.withTransaction
import com.shahriarhasan.usedphoneinspector.core.model.ConditionGrade
import com.shahriarhasan.usedphoneinspector.core.model.DeviceCategory
import com.shahriarhasan.usedphoneinspector.core.model.HistoryFilter
import com.shahriarhasan.usedphoneinspector.core.model.InspectionDraft
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfiles
import com.shahriarhasan.usedphoneinspector.core.model.InspectionStatus
import com.shahriarhasan.usedphoneinspector.core.model.PhysicalCondition
import com.shahriarhasan.usedphoneinspector.core.model.ScoreEngine
import com.shahriarhasan.usedphoneinspector.core.model.ScorableResult
import com.shahriarhasan.usedphoneinspector.core.model.SellerDetails
import com.shahriarhasan.usedphoneinspector.core.model.TestCategory
import com.shahriarhasan.usedphoneinspector.core.model.TestResultTransitions
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class RoomInspectionRepository @Inject constructor(
    private val database: AppDatabase,
    private val dao: InspectionDao,
    private val json: Json,
) : InspectionRepository {
    override fun observeInspection(id: String): Flow<InspectionWithDetails?> = dao.observeInspection(id)
    override fun observeResumable(): Flow<InspectionWithDetails?> = dao.observeResumable()
    override fun observeRecent(limit: Int): Flow<List<InspectionEntity>> = dao.observeRecent(limit)
    override fun observeStats(): Flow<InspectionStats> = dao.observeStats()

    override fun search(filter: HistoryFilter): Flow<List<InspectionWithDetails>> = dao.search(
        query = filter.query.trim(),
        profile = filter.profile?.name,
        grade = filter.grade?.name,
        status = filter.status?.name,
        startDate = filter.startDate,
        endDate = filter.endDate,
        sort = filter.sort.name,
    )

    override suspend fun create(draft: InspectionDraft): String = database.withTransaction {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.upsertInspection(draft.toEntity(id, now))
        val definitions = InspectionProfiles.testsFor(draft.profile)
        dao.upsertTestResults(definitions.map { definition ->
            TestResultEntity(
                id = UUID.randomUUID().toString(),
                inspectionId = id,
                testId = definition.id,
                category = definition.category,
                status = TestStatus.NOT_STARTED,
                required = !definition.optional,
                notes = "",
                issuesJson = "[]",
                readingsJson = "{}",
                durationMillis = 0,
                updatedAt = now,
            )
        })
        id
    }

    override suspend fun updateDraft(id: String, draft: InspectionDraft) {
        val current = requireNotNull(dao.getInspection(id))
        dao.upsertInspection(
            draft.toEntity(id, current.createdAt).copy(
                status = current.status,
                completedAt = current.completedAt,
                lastModifiedAt = if (current.completedAt != null) System.currentTimeMillis() else null,
                reportId = current.reportId,
                conditionScore = current.conditionScore,
                coveragePercent = current.coveragePercent,
                grade = current.grade,
                currentTestId = current.currentTestId,
                completionSnapshotJson = current.completionSnapshotJson,
            ),
        )
    }

    override suspend fun updateCurrentTest(id: String, testId: String) {
        val current = requireNotNull(dao.getInspection(id))
        dao.upsertInspection(
            current.copy(
                status = InspectionStatus.IN_PROGRESS,
                currentTestId = testId,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun saveTestResult(
        inspectionId: String,
        testId: String,
        status: TestStatus,
        notes: String,
        issuesJson: String,
        readingsJson: String,
        durationMillis: Long,
    ) {
        val details = requireNotNull(dao.getInspectionWithDetails(inspectionId))
        val current = requireNotNull(details.testResults.find { it.testId == testId })
        require(TestResultTransitions.canTransition(current.status, status))
        dao.upsertTestResult(
            current.copy(
                status = status,
                notes = notes,
                issuesJson = issuesJson,
                readingsJson = readingsJson,
                durationMillis = durationMillis,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun saveSeller(inspectionId: String, seller: SellerDetails) {
        val existing = dao.getInspectionWithDetails(inspectionId)?.sellers?.firstOrNull()
        dao.upsertSeller(
            SellerEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                inspectionId = inspectionId,
                name = seller.name.trim(),
                phone = seller.phone.trim(),
                alternatePhone = seller.alternatePhone.trim(),
                email = seller.email.trim(),
                businessName = seller.businessName.trim(),
                location = seller.location.trim(),
                address = seller.address.trim(),
                nationalIdReference = seller.nationalIdReference.trim(),
                paymentMethod = seller.paymentMethod.trim(),
                warranty = seller.warranty.trim(),
                purchaseDate = seller.purchaseDate.trim(),
                sellerNotes = seller.sellerNotes.trim(),
                buyerNotes = seller.buyerNotes.trim(),
                repairDetailsJson = seller.repairDetailsJson,
            ),
        )
    }

    override suspend fun savePhysicalChecks(checks: List<PhysicalCheckEntity>) = dao.upsertPhysicalChecks(checks)

    override suspend fun complete(id: String): InspectionEntity = database.withTransaction {
        val details = requireNotNull(dao.getInspectionWithDetails(id))
        val score = ScoreEngine.calculate(
            details.inspection.profile,
            details.testResults
                .filter { it.category !in setOf(TestCategory.IDENTITY, TestCategory.SELLER) }
                .map { ScorableResult(it.category, it.status, it.required) },
        )
        require(score.completedCount > 0) { "At least one required test must be completed" }
        val now = System.currentTimeMillis()
        val reportId = details.inspection.reportId ?: "UPI-${UUID.randomUUID().toString().uppercase().take(12)}"
        val snapshot = exportDetails(details)
        val completed = details.inspection.copy(
            status = InspectionStatus.COMPLETED,
            updatedAt = now,
            completedAt = details.inspection.completedAt ?: now,
            lastModifiedAt = if (details.inspection.completedAt == null) null else now,
            reportId = reportId,
            conditionScore = score.score,
            coveragePercent = score.coveragePercent,
            grade = score.grade,
            completionSnapshotJson = details.inspection.completionSnapshotJson ?: snapshot,
        )
        dao.upsertInspection(completed)
        completed
    }

    override suspend fun reopen(id: String) {
        val current = requireNotNull(dao.getInspection(id))
        dao.upsertInspection(
            current.copy(
                status = InspectionStatus.IN_PROGRESS,
                updatedAt = System.currentTimeMillis(),
                lastModifiedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun duplicate(id: String): String = database.withTransaction {
        val source = requireNotNull(dao.getInspectionWithDetails(id))
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.upsertInspection(
            source.inspection.copy(
                id = newId,
                status = InspectionStatus.DRAFT,
                createdAt = now,
                updatedAt = now,
                completedAt = null,
                lastModifiedAt = null,
                reportId = null,
                conditionScore = null,
                coveragePercent = null,
                grade = null,
                completionSnapshotJson = null,
            ),
        )
        dao.upsertTestResults(source.testResults.map { it.copy(id = UUID.randomUUID().toString(), inspectionId = newId) })
        dao.upsertPhysicalChecks(source.physicalChecks.map { it.copy(id = UUID.randomUUID().toString(), inspectionId = newId) })
        source.sellers.firstOrNull()?.let {
            dao.upsertSeller(it.copy(id = UUID.randomUUID().toString(), inspectionId = newId))
        }
        newId
    }

    override suspend fun delete(id: String) {
        dao.getInspection(id)?.let { dao.deleteInspection(it) }
    }

    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun completedCount(): Int = dao.completedCount()

    override suspend fun exportInspection(id: String): String =
        exportDetails(requireNotNull(dao.getInspectionWithDetails(id)))

    override suspend fun importInspection(json: String): String {
        val backup = try {
            this.json.decodeFromString<InspectionBackup>(json)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("Malformed inspection backup", error)
        }
        require(backup.formatVersion == InspectionBackup.CURRENT_VERSION) { "Unsupported backup version" }
        require(backup.inspection.brand.length <= 200 && backup.inspection.model.length <= 200) { "Invalid backup values" }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        database.withTransaction {
            val source = backup.inspection
            dao.upsertInspection(
                InspectionEntity(
                    id = id,
                    profile = InspectionProfile.valueOf(source.profile),
                    status = InspectionStatus.valueOf(source.status),
                    createdAt = now,
                    updatedAt = now,
                    completedAt = source.completedAt,
                    lastModifiedAt = now,
                    reportId = null,
                    brand = source.brand,
                    model = source.model,
                    deviceCategory = DeviceCategory.valueOf(source.deviceCategory),
                    color = source.color,
                    storageVariant = source.storageVariant,
                    ramVariant = source.ramVariant,
                    askingPrice = source.askingPrice,
                    finalPrice = source.finalPrice,
                    currency = source.currency,
                    purchaseSource = source.purchaseSource,
                    imei1 = source.imei1,
                    imei2 = source.imei2,
                    serialNumber = source.serialNumber,
                    conditionScore = source.conditionScore,
                    coveragePercent = source.coveragePercent,
                    grade = source.grade?.let(ConditionGrade::valueOf),
                    notes = source.notes,
                    scoreConfigVersion = InspectionProfiles.SCORE_CONFIG_VERSION,
                    currentTestId = null,
                    completionSnapshotJson = null,
                ),
            )
            dao.upsertTestResults(backup.tests.map { test ->
                TestResultEntity(
                    id = UUID.randomUUID().toString(),
                    inspectionId = id,
                    testId = test.testId,
                    category = TestCategory.valueOf(test.category),
                    status = TestStatus.valueOf(test.status),
                    required = test.required,
                    notes = test.notes,
                    issuesJson = test.issuesJson,
                    readingsJson = test.readingsJson,
                    durationMillis = test.durationMillis,
                    updatedAt = now,
                )
            })
            dao.upsertPhysicalChecks(backup.physicalChecks.map { check ->
                PhysicalCheckEntity(
                    id = UUID.randomUUID().toString(),
                    inspectionId = id,
                    itemKey = check.itemKey,
                    categoryKey = check.categoryKey,
                    condition = PhysicalCondition.valueOf(check.condition),
                    notes = check.notes,
                )
            })
            backup.seller?.let { seller ->
                saveSeller(
                    id,
                    SellerDetails(
                        name = seller.name,
                        phone = seller.phone,
                        alternatePhone = seller.alternatePhone,
                        email = seller.email,
                        businessName = seller.businessName,
                        location = seller.location,
                        address = seller.address,
                        paymentMethod = seller.paymentMethod,
                        warranty = seller.warranty,
                        purchaseDate = seller.purchaseDate,
                        sellerNotes = seller.sellerNotes,
                        buyerNotes = seller.buyerNotes,
                        repairDetailsJson = seller.repairDetailsJson,
                    ),
                )
            }
        }
        return id
    }

    private fun InspectionDraft.toEntity(id: String, createdAt: Long): InspectionEntity {
        val now = System.currentTimeMillis()
        return InspectionEntity(
            id = id,
            profile = profile,
            status = InspectionStatus.DRAFT,
            createdAt = createdAt,
            updatedAt = now,
            completedAt = null,
            lastModifiedAt = null,
            reportId = null,
            brand = brand.trim(),
            model = model.trim(),
            deviceCategory = deviceCategory,
            color = color.trim(),
            storageVariant = storageVariant.trim(),
            ramVariant = ramVariant.trim(),
            askingPrice = askingPrice.trim(),
            finalPrice = finalPrice.trim(),
            currency = currency.trim().uppercase().take(8),
            purchaseSource = purchaseSource.trim(),
            imei1 = imei1.trim(),
            imei2 = imei2.trim(),
            serialNumber = serialNumber.trim(),
            conditionScore = null,
            coveragePercent = null,
            grade = null,
            notes = notes.trim(),
            scoreConfigVersion = InspectionProfiles.SCORE_CONFIG_VERSION,
            currentTestId = null,
            completionSnapshotJson = null,
        )
    }

    private fun exportDetails(details: InspectionWithDetails): String {
        val inspection = details.inspection
        return json.encodeToString(
            InspectionBackup(
                exportedAt = System.currentTimeMillis(),
                inspection = InspectionEntitySurrogate(
                    id = inspection.id,
                    profile = inspection.profile.name,
                    status = inspection.status.name,
                    createdAt = inspection.createdAt,
                    completedAt = inspection.completedAt,
                    reportId = inspection.reportId,
                    brand = inspection.brand,
                    model = inspection.model,
                    deviceCategory = inspection.deviceCategory.name,
                    color = inspection.color,
                    storageVariant = inspection.storageVariant,
                    ramVariant = inspection.ramVariant,
                    askingPrice = inspection.askingPrice,
                    finalPrice = inspection.finalPrice,
                    currency = inspection.currency,
                    purchaseSource = inspection.purchaseSource,
                    imei1 = inspection.imei1,
                    imei2 = inspection.imei2,
                    serialNumber = inspection.serialNumber,
                    conditionScore = inspection.conditionScore,
                    coveragePercent = inspection.coveragePercent,
                    grade = inspection.grade?.name,
                    notes = inspection.notes,
                ),
                tests = details.testResults.map { test ->
                    TestResultSurrogate(
                        testId = test.testId,
                        category = test.category.name,
                        status = test.status.name,
                        required = test.required,
                        notes = test.notes,
                        issuesJson = test.issuesJson,
                        readingsJson = test.readingsJson,
                        durationMillis = test.durationMillis,
                    )
                },
                physicalChecks = details.physicalChecks.map { check ->
                    PhysicalCheckSurrogate(check.itemKey, check.categoryKey, check.condition.name, check.notes)
                },
                seller = details.sellers.firstOrNull()?.let { seller ->
                    SellerSurrogate(
                        name = seller.name,
                        phone = seller.phone,
                        alternatePhone = seller.alternatePhone,
                        email = seller.email,
                        businessName = seller.businessName,
                        location = seller.location,
                        address = seller.address,
                        paymentMethod = seller.paymentMethod,
                        warranty = seller.warranty,
                        purchaseDate = seller.purchaseDate,
                        sellerNotes = seller.sellerNotes,
                        buyerNotes = seller.buyerNotes,
                        repairDetailsJson = seller.repairDetailsJson,
                    )
                },
            ),
        )
    }
}

