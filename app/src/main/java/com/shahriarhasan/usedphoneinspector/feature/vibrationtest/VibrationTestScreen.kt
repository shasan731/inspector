package com.shahriarhasan.usedphoneinspector.feature.vibrationtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.hardware.VibrationRepository

@Composable
fun VibrationTestScreen(repository: VibrationRepository, onObservation: (String) -> Unit) {
    DisposableEffect(repository) { onDispose(repository::stop) }
    if (!repository.hasVibrator) {
        Text(stringResource(R.string.hardware_unavailable))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.vibration_instruction))
        OutlinedButton(repository::shortPulse, Modifier.fillMaxWidth()) { Text(stringResource(R.string.short_vibration)) }
        OutlinedButton(repository::longPulse, Modifier.fillMaxWidth()) { Text(stringResource(R.string.long_vibration)) }
        OutlinedButton(repository::pattern, Modifier.fillMaxWidth()) { Text(stringResource(R.string.pattern_vibration)) }
        Button(repository::stop, Modifier.fillMaxWidth()) { Text(stringResource(R.string.stop)) }
        listOf(
            "normal" to R.string.vibration_normal,
            "weak" to R.string.vibration_weak,
            "rattling" to R.string.vibration_rattling,
            "intermittent" to R.string.audio_intermittent,
            "not_working" to R.string.vibration_not_working,
        ).forEach { (key, label) ->
            OutlinedButton(onClick = { onObservation(key) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(label))
            }
        }
    }
}

