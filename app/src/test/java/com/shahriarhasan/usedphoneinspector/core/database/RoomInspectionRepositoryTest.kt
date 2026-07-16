package com.shahriarhasan.usedphoneinspector.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shahriarhasan.usedphoneinspector.core.model.InspectionDraft
import com.shahriarhasan.usedphoneinspector.core.model.InspectionStatus
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomInspectionRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomInspectionRepository
    private val json = Json { encodeDefaults = true }

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomInspectionRepository(database, database.inspectionDao(), json)
    }

    @After fun tearDown() { database.close() }

    @Test fun createUpdateResumeCompleteDuplicateAndDelete_work() = runTest {
        val id = repository.create(InspectionDraft(brand = "Acme", model = "One"))
        var details = repository.observeInspection(id).first()
        assertNotNull(details)
        assertEquals(15, details?.testResults?.size)

        val first = requireNotNull(details).testResults.first()
        repository.saveTestResult(id, first.testId, TestStatus.PASS, "checked")
        val completed = repository.complete(id)
        assertEquals(InspectionStatus.COMPLETED, completed.status)
        assertNotNull(completed.reportId)
        assertNotNull(completed.completionSnapshotJson)

        val copyId = repository.duplicate(id)
        assertNotEquals(id, copyId)
        assertEquals(InspectionStatus.DRAFT, repository.observeInspection(copyId).first()?.inspection?.status)
        repository.delete(copyId)
        assertEquals(null, repository.observeInspection(copyId).first())
    }

    @Test fun malformedAndUnsupportedBackups_areRejected() = runTest {
        val malformed = runCatching { repository.importInspection("not-json") }
        assertNotNull(malformed.exceptionOrNull())
        val unsupported = InspectionBackup(
            formatVersion = 99,
            exportedAt = 0,
            inspection = InspectionEntitySurrogate(
                "id", "USED_PHONE", "DRAFT", 0, null, null, "A", "B", "PHONE", "", "", "", "", "", "BDT", "", "", "", "", null, null, null, "",
            ),
            tests = emptyList(),
            physicalChecks = emptyList(),
            seller = null,
        )
        val result = runCatching { repository.importInspection(json.encodeToString(unsupported)) }
        assertNotNull(result.exceptionOrNull())
    }
}

