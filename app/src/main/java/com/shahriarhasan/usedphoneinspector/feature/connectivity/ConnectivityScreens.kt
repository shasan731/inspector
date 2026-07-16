package com.shahriarhasan.usedphoneinspector.feature.connectivity

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.design.TechnicalRow
import com.shahriarhasan.usedphoneinspector.core.hardware.BluetoothScanState
import com.shahriarhasan.usedphoneinspector.core.hardware.BluetoothSnapshot
import com.shahriarhasan.usedphoneinspector.core.hardware.ConnectivityRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.TelephonySnapshot
import com.shahriarhasan.usedphoneinspector.core.hardware.WifiSnapshot
import com.shahriarhasan.usedphoneinspector.core.permissions.PermissionFeature
import com.shahriarhasan.usedphoneinspector.core.permissions.PermissionGate

@Composable
fun WifiTestScreen(snapshot: WifiSnapshot) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.wifi_privacy_notice))
        TechnicalRow(stringResource(R.string.wifi_hardware), stateText(snapshot.available))
        TechnicalRow(stringResource(R.string.wifi_state), stateText(snapshot.enabled))
        TechnicalRow(stringResource(R.string.connected), stateText(snapshot.connected))
        TechnicalRow(stringResource(R.string.validated_connection), stateText(snapshot.validated))
        TechnicalRow(stringResource(R.string.network_name), snapshot.ssid ?: stringResource(R.string.restricted_android))
        TechnicalRow(stringResource(R.string.signal_strength), snapshot.signalLevel?.let { "$it/4" } ?: unavailable())
        TechnicalRow(stringResource(R.string.link_speed), snapshot.linkSpeedMbps?.let { "$it Mbps" } ?: unavailable())
        TechnicalRow(stringResource(R.string.frequency), snapshot.frequencyMhz?.let { "$it MHz" } ?: unavailable())
        TechnicalRow(stringResource(R.string.network_transport), snapshot.transport ?: unavailable())
        Button(
            onClick = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.open_wifi_settings)) }
    }
}

@Composable
fun BluetoothTestScreen(
    snapshot: BluetoothSnapshot,
    repository: ConnectivityRepository,
    onPermissionDenied: () -> Unit,
) {
    val context = LocalContext.current
    PermissionGate(
        feature = PermissionFeature.BLUETOOTH,
        title = R.string.bluetooth_permission_title,
        rationale = R.string.bluetooth_permission_body,
        grantLabel = R.string.continue_action,
        onDenied = onPermissionDenied,
    ) {
        var scanning by remember { mutableStateOf(false) }
        var scanState by remember { mutableStateOf<BluetoothScanState>(BluetoothScanState.Idle) }
        LaunchedEffect(scanning) {
            if (scanning) repository.scanBluetooth().collect {
                scanState = it
                if (it is BluetoothScanState.Finished || it is BluetoothScanState.Error) scanning = false
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TechnicalRow(stringResource(R.string.bluetooth_hardware), stateText(snapshot.available))
            TechnicalRow(stringResource(R.string.bluetooth_state), stateText(snapshot.enabled))
            TechnicalRow(stringResource(R.string.adapter_name), snapshot.adapterName ?: unavailable())
            TechnicalRow(stringResource(R.string.paired_devices), snapshot.pairedCount?.toString() ?: unavailable())
            when (val state = scanState) {
                BluetoothScanState.Idle -> Unit
                is BluetoothScanState.Scanning -> Text(stringResource(R.string.devices_discovered, state.discoveredCount))
                is BluetoothScanState.Finished -> Text(stringResource(R.string.devices_discovered, state.discoveredCount))
                is BluetoothScanState.Error -> Text(stringResource(R.string.operation_failed))
            }
            Button(
                onClick = {
                    if (scanning) {
                        repository.cancelBluetoothScan()
                        scanning = false
                    } else {
                        scanning = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(if (scanning) R.string.cancel_scan else R.string.scan_nearby)) }
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.open_bluetooth_settings)) }
        }
    }
}

@Composable
fun MobileNetworkTestScreen(snapshot: TelephonySnapshot, onPermissionDenied: () -> Unit) {
    val context = LocalContext.current
    if (!snapshot.available) {
        Text(stringResource(R.string.hardware_unavailable))
        return
    }
    PermissionGate(
        feature = PermissionFeature.TELEPHONY,
        title = R.string.telephony_permission_title,
        rationale = R.string.telephony_permission_body,
        grantLabel = R.string.continue_action,
        onDenied = onPermissionDenied,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.mobile_network_title))
            TechnicalRow(stringResource(R.string.sim_slots), snapshot.simSlotCount.toString())
            TechnicalRow(stringResource(R.string.sim_states), snapshot.simStates.joinToString())
            TechnicalRow(stringResource(R.string.carrier), snapshot.carrierName ?: unavailable())
            TechnicalRow(stringResource(R.string.network_type), snapshot.dataNetworkType?.toString() ?: unavailable())
            TechnicalRow(stringResource(R.string.roaming), snapshot.roaming?.let { stateText(it) } ?: unavailable())
            TechnicalRow(stringResource(R.string.airplane_mode), stateText(snapshot.airplaneMode))
            Text(stringResource(R.string.manual_call_sms_notice))
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.open_mobile_settings)) }
        }
    }
}

@Composable
private fun stateText(value: Boolean): String = stringResource(if (value) R.string.enabled else R.string.disabled)

@Composable
private fun unavailable(): String = stringResource(R.string.not_available)
