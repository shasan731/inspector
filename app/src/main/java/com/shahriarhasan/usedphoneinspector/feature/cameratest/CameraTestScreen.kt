package com.shahriarhasan.usedphoneinspector.feature.cameratest

import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.hardware.CameraDescriptor
import com.shahriarhasan.usedphoneinspector.core.hardware.CameraTestController
import com.shahriarhasan.usedphoneinspector.core.permissions.PermissionFeature
import com.shahriarhasan.usedphoneinspector.core.permissions.PermissionGate

@Composable
fun CameraTestScreen(
    cameras: List<CameraDescriptor>,
    controller: CameraTestController,
    onPermissionDenied: () -> Unit,
    onSaveEvidence: (String) -> Unit,
    onObservation: (Set<String>) -> Unit,
) {
    PermissionGate(
        feature = PermissionFeature.CAMERA,
        title = R.string.camera_permission_title,
        rationale = R.string.camera_permission_body,
        grantLabel = R.string.grant_camera,
        onDenied = onPermissionDenied,
    ) {
        CameraPreviewContent(cameras, controller, onSaveEvidence, onObservation)
    }
}

@Composable
private fun CameraPreviewContent(
    cameras: List<CameraDescriptor>,
    controller: CameraTestController,
    onSaveEvidence: (String) -> Unit,
    onObservation: (Set<String>) -> Unit,
) {
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val state by controller.state.collectAsState()
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var observations by remember { mutableStateOf(emptySet<String>()) }
    DisposableEffect(controller) { onDispose(controller::release) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.camera_lenses) + ": " + cameras.size)
        cameras.forEach { descriptor ->
            Text("${descriptor.id} • ${descriptor.focalLengths.joinToString()} mm • ${descriptor.sensorPixelSize.orEmpty()}")
        }
        AndroidView(
            factory = {
                PreviewView(context).also { view ->
                    previewView = view
                    controller.bind(owner, view)
                }
            },
            modifier = Modifier.fillMaxWidth().height(360.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset -> previewView?.let { controller.focus(it, offset.x, offset.y) } }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ -> controller.zoomBy(zoom) }
                },
        )
        state.errorCode?.let { Text(stringResource(R.string.operation_failed)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { previewView?.let { controller.switchCamera(owner, it) } },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.switch_camera)) }
            OutlinedButton(
                onClick = { controller.setTorch(!state.torchEnabled) },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(if (state.torchEnabled) R.string.flash_off else R.string.flash_on)) }
        }
        Button(onClick = controller::capture, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.capture_photo)) }
        state.capturedPath?.let { path ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(controller::clearCapture, Modifier.weight(1f)) { Text(stringResource(R.string.retake)) }
                Button(onClick = { onSaveEvidence(path) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.save_evidence))
                }
            }
        }
        val options = listOf(
            "focus" to R.string.focus_working,
            "sharp" to R.string.image_sharp,
            "lens_damage" to R.string.lens_damaged,
            "dust" to R.string.dust_visible,
            "shaking" to R.string.camera_shaking,
            "color" to R.string.colour_abnormal,
            "black_spot" to R.string.black_spot,
            "flash" to R.string.flash_working,
        )
        options.forEach { (key, label) ->
            FilterChip(
                selected = key in observations,
                onClick = {
                    observations = if (key in observations) observations - key else observations + key
                    onObservation(observations)
                },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

