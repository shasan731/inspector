package com.shahriarhasan.usedphoneinspector.feature.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.design.StatusChip
import com.shahriarhasan.usedphoneinspector.core.model.TestKind
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus
import com.shahriarhasan.usedphoneinspector.feature.audiotest.AudioTestScreen
import com.shahriarhasan.usedphoneinspector.feature.audiotest.MicrophoneTestScreen
import com.shahriarhasan.usedphoneinspector.feature.cameratest.CameraTestScreen
import com.shahriarhasan.usedphoneinspector.feature.chargingtest.BatteryInformationScreen
import com.shahriarhasan.usedphoneinspector.feature.chargingtest.ChargingTestScreen
import com.shahriarhasan.usedphoneinspector.feature.connectivity.BluetoothTestScreen
import com.shahriarhasan.usedphoneinspector.feature.connectivity.MobileNetworkTestScreen
import com.shahriarhasan.usedphoneinspector.feature.connectivity.WifiTestScreen
import com.shahriarhasan.usedphoneinspector.feature.deviceidentity.DeviceInformationScreen
import com.shahriarhasan.usedphoneinspector.feature.displaytest.DisplayTestScreen
import com.shahriarhasan.usedphoneinspector.feature.multitouch.MultitouchScreen
import com.shahriarhasan.usedphoneinspector.feature.physicalinspection.PhysicalInspectionScreen
import com.shahriarhasan.usedphoneinspector.feature.seller.SellerScreen
import com.shahriarhasan.usedphoneinspector.feature.sensortest.SensorTestScreen
import com.shahriarhasan.usedphoneinspector.feature.vibrationtest.VibrationTestScreen

@Composable
fun InspectionWizardScreen(
    state: InspectionUiState,
    viewModel: InspectionViewModel,
    onExit: () -> Unit,
) {
    val test = state.currentTest ?: return
    var confirmExit by remember { mutableStateOf(false) }
    var confirmSkip by remember { mutableStateOf(false) }
    val total = state.tests.size.coerceAtLeast(1)
    val remaining = (total - state.completedCount).coerceAtLeast(0)
    LaunchedEffect(test.id) { viewModel.startCurrentTest() }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { confirmExit = true }) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.save_exit))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(test.category.labelRes), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(test.titleRes), style = MaterialTheme.typography.titleLarge)
            }
            state.currentResult?.let { StatusChip(it.status) }
        }
        LinearProgressIndicator(
            progress = { (state.currentIndex + 1f) / total },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Text(stringResource(R.string.progress_value, state.completedCount, total))
        Text(pluralStringResource(R.plurals.tests_remaining, remaining, remaining))
        val ownsScroll = test.kind in setOf(TestKind.DEVICE_INFO, TestKind.SENSORS, TestKind.PHYSICAL, TestKind.SELLER)
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .then(if (ownsScroll) Modifier else Modifier.verticalScroll(rememberScrollState())),
        ) {
            TestContent(state, viewModel)
        }
        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::setNotes,
            label = { Text(stringResource(R.string.notes)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.showFailureNoteError) {
            Text(stringResource(R.string.failure_notes_required), color = MaterialTheme.colorScheme.error)
        }
        ManualResultControls(
            onPass = { viewModel.saveStatus(TestStatus.PASS) },
            onWarning = { viewModel.saveStatus(TestStatus.WARNING) },
            onFail = { viewModel.saveStatus(TestStatus.FAIL) },
        )
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = viewModel::previous, enabled = state.currentIndex > 0, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Text(stringResource(R.string.previous))
            }
            TextButton(onClick = {
                if (viewModel.settings.value.confirmBeforeSkip) confirmSkip = true else viewModel.saveStatus(TestStatus.SKIPPED)
            }) { Text(stringResource(R.string.skip)) }
            Button(onClick = viewModel::next, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.next))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.exit_confirmation_title)) },
            text = { Text(stringResource(R.string.exit_confirmation_body)) },
            confirmButton = { TextButton(onClick = onExit) { Text(stringResource(R.string.save_exit)) } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmSkip) {
        AlertDialog(
            onDismissRequest = { confirmSkip = false },
            title = { Text(stringResource(R.string.skip_confirmation)) },
            confirmButton = {
                TextButton(onClick = { confirmSkip = false; viewModel.saveStatus(TestStatus.SKIPPED) }) {
                    Text(stringResource(R.string.skip))
                }
            },
            dismissButton = { TextButton(onClick = { confirmSkip = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun TestContent(state: InspectionUiState, viewModel: InspectionViewModel) {
    val details = state.details ?: return
    when (state.currentTest?.kind) {
        TestKind.DEVICE_INFO -> DeviceInformationScreen(viewModel.deviceInfo)
        TestKind.DISPLAY -> DisplayTestScreen(state.issues, viewModel::setIssues)
        TestKind.MULTITOUCH -> MultitouchScreen { maximum, coverage ->
            viewModel.putReading("maximum_touches", maximum.toString())
            viewModel.putReading("touch_coverage", coverage.toString())
        }
        TestKind.SPEAKER -> AudioTestScreen(viewModel.speakerController) { viewModel.putReading("observation", it) }
        TestKind.MICROPHONE -> MicrophoneTestScreen(viewModel.microphoneController, viewModel::markPermissionDenied)
        TestKind.CAMERA -> CameraTestScreen(
            cameras = viewModel.cameras,
            controller = viewModel.cameraController,
            onPermissionDenied = viewModel::markPermissionDenied,
            onSaveEvidence = viewModel::saveCameraEvidence,
            onObservation = viewModel::setIssues,
        )
        TestKind.VIBRATION -> VibrationTestScreen(viewModel.vibrationRepository) { viewModel.putReading("observation", it) }
        TestKind.SENSORS -> SensorTestScreen(viewModel.sensorRepository) { viewModel.putReading("change_detected", it.toString()) }
        TestKind.CHARGING -> ChargingTestScreen(viewModel.battery.value)
        TestKind.BATTERY -> BatteryInformationScreen(viewModel.battery.value)
        TestKind.WIFI -> WifiTestScreen(viewModel.connectivityRepository.wifiSnapshot())
        TestKind.BLUETOOTH -> BluetoothTestScreen(
            viewModel.connectivityRepository.bluetoothSnapshot(),
            viewModel.connectivityRepository,
            viewModel::markPermissionDenied,
        )
        TestKind.MOBILE -> MobileNetworkTestScreen(viewModel.telephonyRepository.snapshot(), viewModel::markPermissionDenied)
        TestKind.PHYSICAL -> PhysicalInspectionScreen(details.physicalChecks, viewModel::setPhysicalDrafts)
        TestKind.SELLER -> SellerScreen(details.inspection.profile, state.seller, viewModel::setSeller)
        null -> Unit
    }
}

@Composable
internal fun ManualResultControls(onPass: () -> Unit, onWarning: () -> Unit, onFail: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onPass, Modifier.weight(1f)) { Text(stringResource(R.string.mark_pass)) }
        OutlinedButton(onWarning, Modifier.weight(1f)) { Text(stringResource(R.string.mark_warning)) }
        OutlinedButton(onFail, Modifier.weight(1f)) { Text(stringResource(R.string.mark_fail)) }
    }
}
