package com.shahriarhasan.usedphoneinspector.feature.audiotest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.hardware.MicrophoneTestController
import com.shahriarhasan.usedphoneinspector.core.hardware.RecorderStatus
import com.shahriarhasan.usedphoneinspector.core.permissions.PermissionFeature
import com.shahriarhasan.usedphoneinspector.core.permissions.PermissionGate

@Composable
fun MicrophoneTestScreen(controller: MicrophoneTestController, onPermissionDenied: () -> Unit) {
    if (!controller.hasMicrophone) {
        Text(stringResource(R.string.hardware_unavailable))
        return
    }
    PermissionGate(
        feature = PermissionFeature.MICROPHONE,
        title = R.string.microphone_permission_title,
        rationale = R.string.microphone_permission_body,
        grantLabel = R.string.grant_microphone,
        onDenied = onPermissionDenied,
    ) {
        MicrophoneRecorder(controller)
    }
}

@Composable
private fun MicrophoneRecorder(controller: MicrophoneTestController) {
    val state by controller.state.collectAsState()
    DisposableEffect(controller) { onDispose(controller::release) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.temporary_audio_notice))
        Text(stringResource(R.string.recording_timer, state.elapsedMillis / 1_000f))
        Text(stringResource(R.string.input_level))
        LinearProgressIndicator(
            progress = { (state.amplitude / 32767f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(12.dp),
        )
        when (state.status) {
            RecorderStatus.IDLE -> Button(onClick = { controller.start() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.record))
            }
            RecorderStatus.RECORDING -> Button(onClick = controller::stop, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.stop))
            }
            RecorderStatus.RECORDED,
            RecorderStatus.PLAYING -> {
                Button(onClick = controller::play, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.play)) }
                OutlinedButton(onClick = { controller.start() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.record_again))
                }
                OutlinedButton(onClick = controller::delete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete))
                }
                OutlinedButton(onClick = { controller.retainAsEvidence() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.retain_audio))
                }
            }
            RecorderStatus.ERROR -> {
                Text(stringResource(R.string.operation_failed))
                Button(onClick = { controller.start() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}

