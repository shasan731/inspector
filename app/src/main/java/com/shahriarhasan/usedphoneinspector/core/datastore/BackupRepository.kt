package com.shahriarhasan.usedphoneinspector.core.datastore

import com.shahriarhasan.usedphoneinspector.core.database.InspectionBackup
import com.shahriarhasan.usedphoneinspector.core.database.InspectionDao
import com.shahriarhasan.usedphoneinspector.core.database.InspectionRepository
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ApplicationBackup(
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val inspections: List<InspectionBackup>,
)

class BackupRepository @Inject constructor(
    private val dao: InspectionDao,
    private val inspectionRepository: InspectionRepository,
    private val json: Json,
) {
    suspend fun exportAll(): String {
        val inspections = dao.getAllWithDetails().map { details ->
            json.decodeFromString<InspectionBackup>(inspectionRepository.exportInspection(details.inspection.id))
        }
        return json.encodeToString(ApplicationBackup(exportedAt = System.currentTimeMillis(), inspections = inspections))
    }

    suspend fun importAll(raw: String): Int {
        require(raw.length <= MAX_BACKUP_CHARS) { "Backup is too large" }
        val backup = try {
            json.decodeFromString<ApplicationBackup>(raw)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("Malformed application backup", error)
        }
        require(backup.formatVersion == 1) { "Unsupported backup version" }
        require(backup.inspections.size <= 10_000) { "Backup contains too many inspections" }
        backup.inspections.forEach { inspectionRepository.importInspection(json.encodeToString(it)) }
        return backup.inspections.size
    }

    private companion object { const val MAX_BACKUP_CHARS = 50_000_000 }
}

