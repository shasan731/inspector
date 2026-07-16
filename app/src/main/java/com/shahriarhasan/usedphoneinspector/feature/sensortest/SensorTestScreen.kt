package com.shahriarhasan.usedphoneinspector.feature.sensortest

import android.hardware.Sensor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.design.TechnicalRow
import com.shahriarhasan.usedphoneinspector.core.hardware.SensorDescriptor
import com.shahriarhasan.usedphoneinspector.core.hardware.SensorRepository
import java.util.Locale
import kotlin.math.abs

@Composable
fun SensorTestScreen(repository: SensorRepository, onChangeDetected: (Boolean) -> Unit) {
    val sensors = remember(repository) { repository.sensors() }
    val dedicated = listOf(
        Sensor.TYPE_ACCELEROMETER to R.string.accelerometer,
        Sensor.TYPE_GYROSCOPE to R.string.gyroscope,
        Sensor.TYPE_PROXIMITY to R.string.proximity,
        Sensor.TYPE_LIGHT to R.string.light_sensor,
        Sensor.TYPE_MAGNETIC_FIELD to R.string.magnetometer,
    )
    var selectedType by remember { mutableStateOf(dedicated.first().first) }
    val flow = remember(selectedType) { repository.observe(selectedType) }
    val reading by flow.collectAsState(initial = null)
    var baseline by remember(selectedType) { mutableStateOf<List<Float>?>(null) }
    val changed = reading?.values?.let { values ->
        val first = baseline
        if (first == null) {
            baseline = values
            false
        } else {
            values.zip(first).any { (current, start) -> abs(current - start) > meaningfulThreshold(selectedType) }
        }
    } ?: false
    LaunchedEffect(changed) { if (changed) onChangeDetected(true) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.sensor_interactive_tests), style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dedicated.take(3).forEach { (type, label) ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dedicated.drop(3).forEach { (type, label) ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        if (sensors.none { it.type == selectedType }) {
            Text(stringResource(R.string.hardware_unavailable))
        } else {
            Text(instructionFor(selectedType))
            reading?.let {
                Text(stringResource(R.string.sensor_values, it.values.joinToString { value -> "%.2f".format(Locale.US, value) }))
            }
            Text(stringResource(if (changed) R.string.change_detected else R.string.waiting_for_change))
        }
        if (selectedType == Sensor.TYPE_MAGNETIC_FIELD) Text(stringResource(R.string.magnet_warning))
        Text(stringResource(R.string.sensor_inventory), style = MaterialTheme.typography.titleLarge)
        Card(Modifier.fillMaxWidth()) {
            LazyColumn {
                items(sensors, key = { "${it.type}-${it.name}" }) { sensor -> SensorRow(sensor) }
            }
        }
    }
}

@Composable
private fun SensorRow(sensor: SensorDescriptor) {
    Column {
        Text(
            stringResource(R.string.sensor_name_vendor, sensor.name, sensor.vendor),
            style = MaterialTheme.typography.titleSmall,
        )
        TechnicalRow(stringResource(R.string.sensor_type, sensor.type), "v${sensor.version}")
        TechnicalRow("Range / resolution", "${sensor.maximumRange} / ${sensor.resolution}")
        TechnicalRow("Power / delay", "${sensor.powerMilliamp} mA / ${sensor.minimumDelayMicroseconds} µs")
        TechnicalRow("Mode / wake-up", "${sensor.reportingMode} / ${sensor.isWakeUpSensor}")
        HorizontalDivider()
    }
}

@Composable
private fun instructionFor(type: Int): String = when (type) {
    Sensor.TYPE_PROXIMITY -> stringResource(R.string.proximity_instruction)
    Sensor.TYPE_LIGHT -> stringResource(R.string.light_instruction)
    else -> stringResource(R.string.sensor_move_instruction)
}

private fun meaningfulThreshold(type: Int): Float = when (type) {
    Sensor.TYPE_LIGHT -> 5f
    Sensor.TYPE_PROXIMITY -> 0.5f
    Sensor.TYPE_MAGNETIC_FIELD -> 5f
    else -> 0.3f
}

