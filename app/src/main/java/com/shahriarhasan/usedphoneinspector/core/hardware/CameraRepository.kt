package com.shahriarhasan.usedphoneinspector.core.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class CameraDescriptor(
    val id: String,
    val lensFacing: Int?,
    val focalLengths: List<Float>,
    val sensorPixelSize: String?,
    val flashAvailable: Boolean,
    val hardwareLevel: Int?,
)

interface CameraRepository {
    fun cameras(): List<CameraDescriptor>
    fun setTorch(enabled: Boolean): Boolean
    fun turnOffTorch()
}

@Singleton
class AndroidCameraRepository @Inject constructor(
    @ApplicationContext context: Context,
) : CameraRepository {
    private val manager = context.getSystemService(CameraManager::class.java)
    private var torchCameraId: String? = null

    override fun cameras(): List<CameraDescriptor> = runCatching {
        manager.cameraIdList.map { id ->
            val characteristics = manager.getCameraCharacteristics(id)
            val size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            CameraDescriptor(
                id = id,
                lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING),
                focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList().orEmpty(),
                sensorPixelSize = size?.let { "${it.width}×${it.height}" },
                flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true,
                hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL),
            )
        }
    }.getOrDefault(emptyList())

    @SuppressLint("MissingPermission")
    override fun setTorch(enabled: Boolean): Boolean {
        val id = torchCameraId ?: cameras().firstOrNull { it.flashAvailable }?.id ?: return false
        return runCatching {
            manager.setTorchMode(id, enabled)
            torchCameraId = if (enabled) id else null
            true
        }.getOrDefault(false)
    }

    override fun turnOffTorch() {
        torchCameraId?.let { id -> runCatching { manager.setTorchMode(id, false) } }
        torchCameraId = null
    }
}

