package com.shahriarhasan.usedphoneinspector.core.hardware

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CameraTestState(
    val isBound: Boolean = false,
    val isFrontCamera: Boolean = false,
    val zoomRatio: Float = 1f,
    val torchEnabled: Boolean = false,
    val capturedPath: String? = null,
    val errorCode: String? = null,
)

interface CameraTestController {
    val state: StateFlow<CameraTestState>
    fun bind(owner: LifecycleOwner, previewView: PreviewView)
    fun switchCamera(owner: LifecycleOwner, previewView: PreviewView)
    fun focus(previewView: PreviewView, x: Float, y: Float)
    fun zoomBy(scale: Float)
    fun capture()
    fun setTorch(enabled: Boolean)
    fun clearCapture()
    fun release()
}

@Singleton
class CameraXTestController @Inject constructor(
    @ApplicationContext private val context: Context,
) : CameraTestController {
    private val mutableState = MutableStateFlow(CameraTestState())
    override val state: StateFlow<CameraTestState> = mutableState
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null

    @SuppressLint("MissingPermission")
    override fun bind(owner: LifecycleOwner, previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                provider = future.get()
                bindNow(owner, previewView)
            }.onFailure {
                mutableState.value = mutableState.value.copy(errorCode = "INITIALIZATION_FAILED")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("MissingPermission")
    private fun bindNow(owner: LifecycleOwner, previewView: PreviewView) {
        val selector = if (mutableState.value.isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val capture = ImageCapture.Builder().setFlashMode(ImageCapture.FLASH_MODE_AUTO).build()
        provider?.unbindAll()
        camera = provider?.bindToLifecycle(owner, selector, preview, capture)
        imageCapture = capture
        mutableState.value = mutableState.value.copy(isBound = true, errorCode = null)
    }

    override fun switchCamera(owner: LifecycleOwner, previewView: PreviewView) {
        mutableState.value = mutableState.value.copy(isFrontCamera = !mutableState.value.isFrontCamera)
        bindNow(owner, previewView)
    }

    override fun focus(previewView: PreviewView, x: Float, y: Float) {
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    override fun zoomBy(scale: Float) {
        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return
        val target = (zoomState.zoomRatio * scale).coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        camera?.cameraControl?.setZoomRatio(target)
        mutableState.value = mutableState.value.copy(zoomRatio = target)
    }

    override fun capture() {
        val capture = imageCapture ?: return
        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(directory, "camera-${UUID.randomUUID()}.jpg")
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    mutableState.value = mutableState.value.copy(capturedPath = file.absolutePath, errorCode = null)
                }
                override fun onError(exception: ImageCaptureException) {
                    file.delete()
                    mutableState.value = mutableState.value.copy(errorCode = "CAPTURE_FAILED")
                }
            },
        )
    }

    override fun setTorch(enabled: Boolean) {
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
        if (!hasFlash) {
            mutableState.value = mutableState.value.copy(errorCode = "FLASH_UNSUPPORTED")
            return
        }
        camera?.cameraControl?.enableTorch(enabled)
        mutableState.value = mutableState.value.copy(torchEnabled = enabled)
    }

    override fun clearCapture() {
        mutableState.value.capturedPath?.let(::File)?.delete()
        mutableState.value = mutableState.value.copy(capturedPath = null)
    }

    override fun release() {
        setTorch(false)
        provider?.unbindAll()
        camera = null
        imageCapture = null
        mutableState.value = CameraTestState(capturedPath = mutableState.value.capturedPath)
    }
}

