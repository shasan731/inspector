package com.shahriarhasan.usedphoneinspector.feature.deviceidentity

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.shahriarhasan.usedphoneinspector.core.hardware.DeviceInfoSnapshot

@Composable
fun DeviceInformationScreen(snapshot: DeviceInfoSnapshot, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var advanced by remember { mutableStateOf(false) }
    val values = listOf(
        R.string.manufacturer to snapshot.manufacturer,
        R.string.brand to snapshot.brand,
        R.string.model to snapshot.model,
        R.string.product_name to snapshot.product,
        R.string.android_version to snapshot.androidVersion,
        R.string.api_level to snapshot.apiLevel.toString(),
        R.string.security_patch to snapshot.securityPatch.ifBlank { stringResource(R.string.not_available) },
        R.string.cpu_abis to snapshot.cpuAbis.joinToString(),
        R.string.total_ram to Formatter.formatFileSize(context, snapshot.totalRamBytes),
        R.string.available_ram to Formatter.formatFileSize(context, snapshot.availableRamBytes),
        R.string.total_storage to Formatter.formatFileSize(context, snapshot.totalStorageBytes),
        R.string.available_storage to Formatter.formatFileSize(context, snapshot.availableStorageBytes),
        R.string.screen_resolution to "${snapshot.screenWidthPixels} × ${snapshot.screenHeightPixels}",
        R.string.screen_density to "${snapshot.densityDpi} dpi",
        R.string.refresh_rate to "${snapshot.currentRefreshRate.toInt()} Hz",
        R.string.available_refresh_rates to snapshot.availableRefreshRates.joinToString { "${it.toInt()} Hz" },
        R.string.camera_count to snapshot.cameraCount.toString(),
        R.string.cellular_hardware to booleanText(snapshot.cellularAvailable),
        R.string.bluetooth_hardware to booleanText(snapshot.bluetoothAvailable),
        R.string.wifi_hardware to booleanText(snapshot.wifiAvailable),
        R.string.nfc_hardware to booleanText(snapshot.nfcAvailable),
        R.string.flashlight_hardware to booleanText(snapshot.flashlightAvailable),
        R.string.sensor_count to snapshot.sensorCount.toString(),
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.device_information_title), style = MaterialTheme.typography.titleLarge)
        Card(Modifier.fillMaxWidth()) {
            LazyColumn {
                items(values) { (label, value) ->
                    TechnicalRow(stringResource(label), value)
                    HorizontalDivider()
                }
                item {
                    androidx.compose.material3.TextButton(onClick = { advanced = !advanced }) {
                        Text(stringResource(R.string.advanced_information))
                    }
                }
                if (advanced) {
                    item { TechnicalRow(stringResource(R.string.build_fingerprint), snapshot.fingerprint) }
                }
            }
        }
    }
}

@Composable
private fun booleanText(value: Boolean): String = stringResource(if (value) R.string.detected else R.string.unsupported)

