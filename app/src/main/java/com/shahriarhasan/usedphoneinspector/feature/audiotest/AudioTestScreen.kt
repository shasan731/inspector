package com.shahriarhasan.usedphoneinspector.feature.audiotest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.hardware.SpeakerSample
import com.shahriarhasan.usedphoneinspector.core.hardware.SpeakerTestController

@Composable
fun AudioTestScreen(controller: SpeakerTestController, onObservation: (String) -> Unit) {
    var volume by remember { mutableFloatStateOf(0.7f) }
    var selected by remember { mutableStateOf<String?>(null) }
    val samples = listOf(
        SpeakerSample.SPOKEN to R.string.spoken_sample,
        SpeakerSample.LOW_TONE to R.string.low_tone,
        SpeakerSample.MID_TONE to R.string.mid_tone,
        SpeakerSample.HIGH_TONE to R.string.high_tone,
        SpeakerSample.STEREO to R.string.stereo_sample,
    )
    val observations = listOf(
        "clear" to R.string.audio_clear,
        "distorted" to R.string.audio_distorted,
        "quiet" to R.string.audio_quiet,
        "channel_missing" to R.string.audio_channel_missing,
        "intermittent" to R.string.audio_intermittent,
    )
    DisposableEffect(controller) { onDispose(controller::stop) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.speaker_instruction))
        samples.forEach { (sample, label) ->
            OutlinedButton(
                onClick = { controller.play(sample, volume) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(label)) }
        }
        Text(stringResource(R.string.volume), style = MaterialTheme.typography.labelLarge)
        Slider(value = volume, onValueChange = { volume = it })
        Button(onClick = controller::stop, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.stop)) }
        Text(stringResource(R.string.manual_result), style = MaterialTheme.typography.titleMedium)
        observations.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (key, label) ->
                    OutlinedButton(
                        onClick = { selected = key; onObservation(key) },
                        modifier = Modifier.weight(1f),
                    ) { Text(if (selected == key) "✓ ${stringResource(label)}" else stringResource(label)) }
                }
            }
        }
    }
}

