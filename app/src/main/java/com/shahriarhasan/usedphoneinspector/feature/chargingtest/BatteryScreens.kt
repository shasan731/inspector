package com.shahriarhasan.usedphoneinspector.feature.chargingtest

import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.design.InformationCard
import com.shahriarhasan.usedphoneinspector.core.design.TechnicalRow
import com.shahriarhasan.usedphoneinspector.core.hardware.BatterySnapshot
import java.util.Locale

@Composable
fun ChargingTestScreen(snapshot: BatterySnapshot?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InformationCard(stringResource(R.string.test_charging), stringResource(R.string.charging_instruction))
        BatteryReadings(snapshot, showEstimate = true)
        Text(stringResource(R.string.estimate_warning), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun BatteryInformationScreen(snapshot: BatterySnapshot?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.test_battery_information), style = MaterialTheme.typography.titleLarge)
        BatteryReadings(snapshot, showEstimate = false)
        TechnicalRow(stringResource(R.string.not_exposed), stringResource(R.string.not_exposed))
    }
}

@Composable
private fun BatteryReadings(snapshot: BatterySnapshot?, showEstimate: Boolean) {
    if (snapshot == null) {
        Text(stringResource(R.string.loading))
        return
    }
    TechnicalRow(stringResource(R.string.battery_level), snapshot.levelPercent?.let { "$it%" } ?: stringResource(R.string.not_available))
    TechnicalRow(
        stringResource(R.string.charging_state),
        stringResource(if (snapshot.isCharging) R.string.charging else R.string.not_charging),
    )
    TechnicalRow(stringResource(R.string.plug_type), plugName(snapshot.pluggedType))
    TechnicalRow(stringResource(R.string.battery_status), snapshot.status.toString())
    TechnicalRow(stringResource(R.string.battery_health), snapshot.health.toString())
    TechnicalRow(stringResource(R.string.temperature), snapshot.temperatureCelsius?.let { "%.1f °C".format(Locale.US, it) } ?: unavailable())
    TechnicalRow(stringResource(R.string.voltage), snapshot.voltageMillivolts?.let { "$it mV" } ?: unavailable())
    TechnicalRow(stringResource(R.string.current_now), snapshot.currentNowMicroamps?.let { "$it µA" } ?: unavailable())
    TechnicalRow(stringResource(R.string.current_average), snapshot.currentAverageMicroamps?.let { "$it µA" } ?: unavailable())
    TechnicalRow(stringResource(R.string.charge_counter), snapshot.chargeCounterMicroampHours?.let { "$it µAh" } ?: unavailable())
    TechnicalRow(stringResource(R.string.battery_technology), snapshot.technology ?: unavailable())
    TechnicalRow(stringResource(R.string.battery_saver), stringResource(if (snapshot.powerSaveMode) R.string.enabled else R.string.disabled))
    TechnicalRow(stringResource(R.string.charge_time_remaining), snapshot.chargeTimeRemainingMillis?.let { "${it / 60_000} min" } ?: unavailable())
    if (showEstimate) {
        TechnicalRow(
            stringResource(R.string.estimated_wattage),
            snapshot.estimatedWatts?.let { "%.2f W".format(Locale.US, it) } ?: unavailable(),
        )
    }
}

@Composable
private fun plugName(value: Int): String = when (value) {
    BatteryManager.BATTERY_PLUGGED_AC -> "AC"
    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
    BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
    else -> stringResource(R.string.not_available)
}

@Composable
private fun unavailable(): String = stringResource(R.string.not_exposed)

