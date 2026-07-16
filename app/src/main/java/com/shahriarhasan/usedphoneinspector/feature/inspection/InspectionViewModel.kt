package com.shahriarhasan.usedphoneinspector.feature.inspection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahriarhasan.usedphoneinspector.core.database.InspectionRepository
import com.shahriarhasan.usedphoneinspector.core.database.InspectionWithDetails
import com.shahriarhasan.usedphoneinspector.core.database.PhysicalCheckEntity
import com.shahriarhasan.usedphoneinspector.core.datastore.AppSettings
import com.shahriarhasan.usedphoneinspector.core.datastore.SettingsRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.AndroidMicrophoneTestController
import com.shahriarhasan.usedphoneinspector.core.hardware.AndroidSpeakerTestController
import com.shahriarhasan.usedphoneinspector.core.hardware.BatteryRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.BatterySnapshot
import com.shahriarhasan.usedphoneinspector.core.hardware.CameraRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.CameraTestController
import com.shahriarhasan.usedphoneinspector.core.hardware.ConnectivityRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.DeviceInfoRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.DeviceInfoSnapshot
import com.shahriarhasan.usedphoneinspector.core.hardware.SensorRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.TelephonyRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.VibrationRepository
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfiles
import com.shahriarhasan.usedphoneinspector.core.model.PhotoType
import com.shahriarhasan.usedphoneinspector.core.model.SellerDetails
import com.shahriarhasan.usedphoneinspector.core.model.TestDefinition
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus
import com.shahriarhasan.usedphoneinspector.core.reporting.EvidenceRepository
import com.shahriarhasan.usedphoneinspector.feature.physicalinspection.PhysicalDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class InspectionUiState(
    val details: InspectionWithDetails? = null,
    val tests: List<TestDefinition> = emptyList(),
    val currentIndex: Int = 0,
    val notes: String = "",
    val issues: Set<String> = emptySet(),
    val readings: Map<String, String> = emptyMap(),
    val seller: SellerDetails = SellerDetails(),
    val physicalDrafts: Map<String, PhysicalDraft> = emptyMap(),
    val showFailureNoteError: Boolean = false,
    val isSavingEvidence: Boolean = false,
) {
    val currentTest: TestDefinition? get() = tests.getOrNull(currentIndex)
    val currentResult get() = details?.testResults?.firstOrNull { it.testId == currentTest?.id }
    val completedCount get() = details?.testResults?.count { it.status.isTerminal } ?: 0
}

sealed interface InspectionNavigation {
    data object Review : InspectionNavigation
}

@HiltViewModel
class InspectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: InspectionRepository,
    settingsRepository: SettingsRepository,
    deviceInfoRepository: DeviceInfoRepository,
    batteryRepository: BatteryRepository,
    val sensorRepository: SensorRepository,
    val connectivityRepository: ConnectivityRepository,
    val telephonyRepository: TelephonyRepository,
    val vibrationRepository: VibrationRepository,
    cameraRepository: CameraRepository,
    val cameraController: CameraTestController,
    val speakerController: AndroidSpeakerTestController,
    val microphoneController: AndroidMicrophoneTestController,
    private val evidenceRepository: EvidenceRepository,
    private val json: Json,
) : ViewModel() {
    val inspectionId: String = requireNotNull(savedStateHandle["inspectionId"])
    private val mutableState = MutableStateFlow(InspectionUiState())
    val state: StateFlow<InspectionUiState> = mutableState
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val deviceInfo: DeviceInfoSnapshot = deviceInfoRepository.snapshot()
    val battery = batteryRepository.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val cameras = cameraRepository.cameras()
    private val navigationChannel = Channel<InspectionNavigation>(Channel.BUFFERED)
    val navigation = navigationChannel.receiveAsFlow()
    private var testStartedAt = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            repository.observeInspection(inspectionId).collectLatest { details ->
                if (details == null) return@collectLatest
                val tests = InspectionProfiles.testsFor(details.inspection.profile, deviceInfo.cellularAvailable)
                val requestedId = details.inspection.currentTestId
                val newIndex = tests.indexOfFirst { it.id == requestedId }.takeIf { it >= 0 }
                    ?: mutableState.value.currentIndex.coerceIn(0, (tests.size - 1).coerceAtLeast(0))
                val testChanged = mutableState.value.currentTest?.id != tests.getOrNull(newIndex)?.id
                val seller = details.sellers.firstOrNull()?.toDetails() ?: mutableState.value.seller
                mutableState.value = mutableState.value.copy(
                    details = details,
                    tests = tests,
                    currentIndex = newIndex,
                    notes = if (testChanged) details.testResults.firstOrNull { it.testId == tests.getOrNull(newIndex)?.id }?.notes.orEmpty()
                        else mutableState.value.notes,
                    seller = seller,
                )
            }
        }
    }

    fun startCurrentTest() {
        val test = mutableState.value.currentTest ?: return
        testStartedAt = System.currentTimeMillis()
        viewModelScope.launch {
            repository.updateCurrentTest(inspectionId, test.id)
            if (mutableState.value.currentResult?.status == TestStatus.NOT_STARTED) {
                repository.saveTestResult(inspectionId, test.id, TestStatus.IN_PROGRESS, mutableState.value.notes)
            }
        }
    }

    fun setNotes(notes: String) {
        mutableState.value = mutableState.value.copy(notes = notes.take(2_000), showFailureNoteError = false)
    }

    fun setIssues(issues: Set<String>) { mutableState.value = mutableState.value.copy(issues = issues) }

    fun putReading(key: String, value: String) {
        mutableState.value = mutableState.value.copy(readings = mutableState.value.readings + (key to value))
    }

    fun setSeller(seller: SellerDetails) { mutableState.value = mutableState.value.copy(seller = seller) }

    fun setPhysicalDrafts(drafts: Map<String, PhysicalDraft>) {
        mutableState.value = mutableState.value.copy(physicalDrafts = drafts)
    }

    fun saveStatus(status: TestStatus) {
        val current = mutableState.value
        val test = current.currentTest ?: return
        if (status == TestStatus.FAIL && settings.value.requireFailureNotes && current.notes.isBlank()) {
            mutableState.value = current.copy(showFailureNoteError = true)
            return
        }
        viewModelScope.launch {
            persistFeatureData()
            repository.saveTestResult(
                inspectionId = inspectionId,
                testId = test.id,
                status = status,
                notes = current.notes,
                issuesJson = json.encodeToString(current.issues),
                readingsJson = json.encodeToString(current.readings),
                durationMillis = System.currentTimeMillis() - testStartedAt,
            )
        }
    }

    fun markPermissionDenied() = saveStatus(TestStatus.PERMISSION_DENIED)

    fun retry() {
        saveStatus(TestStatus.IN_PROGRESS)
        testStartedAt = System.currentTimeMillis()
    }

    fun previous() = moveTo(mutableState.value.currentIndex - 1)

    fun next() {
        val current = mutableState.value
        if (current.currentIndex >= current.tests.lastIndex) {
            viewModelScope.launch {
                persistFeatureData()
                navigationChannel.send(InspectionNavigation.Review)
            }
        } else moveTo(current.currentIndex + 1)
    }

    fun saveCameraEvidence(path: String) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSavingEvidence = true)
            runCatching { evidenceRepository.storeFile(inspectionId, File(path), PhotoType.CAMERA_SAMPLE) }
            mutableState.value = mutableState.value.copy(isSavingEvidence = false)
        }
    }

    private fun moveTo(index: Int) {
        val state = mutableState.value
        val bounded = index.coerceIn(0, state.tests.lastIndex)
        val test = state.tests[bounded]
        viewModelScope.launch {
            persistFeatureData()
            repository.updateCurrentTest(inspectionId, test.id)
            mutableState.value = mutableState.value.copy(
                currentIndex = bounded,
                notes = state.details?.testResults?.firstOrNull { it.testId == test.id }?.notes.orEmpty(),
                issues = emptySet(),
                readings = emptyMap(),
                showFailureNoteError = false,
            )
            startCurrentTest()
        }
    }

    private suspend fun persistFeatureData() {
        val current = mutableState.value
        if (current.currentTest?.id == "seller_information") repository.saveSeller(inspectionId, current.seller)
        if (current.currentTest?.id == "physical_checklist" && current.physicalDrafts.isNotEmpty()) {
            val existing = current.details?.physicalChecks.orEmpty().associateBy { it.itemKey }
            repository.savePhysicalChecks(
                current.physicalDrafts.map { (itemKey, draft) ->
                    PhysicalCheckEntity(
                        id = existing[itemKey]?.id ?: UUID.randomUUID().toString(),
                        inspectionId = inspectionId,
                        itemKey = itemKey,
                        categoryKey = draft.categoryKey,
                        condition = draft.condition,
                        notes = draft.notes,
                    )
                },
            )
        }
    }

    private fun com.shahriarhasan.usedphoneinspector.core.database.SellerEntity.toDetails() = SellerDetails(
        name = name,
        phone = phone,
        alternatePhone = alternatePhone,
        email = email,
        businessName = businessName,
        location = location,
        address = address,
        nationalIdReference = nationalIdReference,
        paymentMethod = paymentMethod,
        warranty = warranty,
        purchaseDate = purchaseDate,
        sellerNotes = sellerNotes,
        buyerNotes = buyerNotes,
        repairDetailsJson = repairDetailsJson,
    )
}

