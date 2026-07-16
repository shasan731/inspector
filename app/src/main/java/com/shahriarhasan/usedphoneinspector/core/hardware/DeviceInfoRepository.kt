package com.shahriarhasan.usedphoneinspector.core.hardware

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceInfoSnapshot(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val product: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val fingerprint: String,
    val cpuAbis: List<String>,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val totalStorageBytes: Long,
    val availableStorageBytes: Long,
    val screenWidthPixels: Int,
    val screenHeightPixels: Int,
    val densityDpi: Int,
    val currentRefreshRate: Float,
    val availableRefreshRates: List<Float>,
    val cameraCount: Int,
    val cellularAvailable: Boolean,
    val bluetoothAvailable: Boolean,
    val wifiAvailable: Boolean,
    val nfcAvailable: Boolean,
    val flashlightAvailable: Boolean,
    val sensorCount: Int,
)

interface DeviceInfoRepository {
    fun snapshot(): DeviceInfoSnapshot
}

@Singleton
class AndroidDeviceInfoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceInfoRepository {
    override fun snapshot(): DeviceInfoSnapshot {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val storage = StatFs(Environment.getDataDirectory().absolutePath)
        val windowManager = context.getSystemService(WindowManager::class.java)
        @Suppress("DEPRECATION")
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display ?: windowManager.defaultDisplay
        } else {
            windowManager.defaultDisplay
        }
        val mode = display.mode
        val metrics = context.resources.displayMetrics
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val cameraIds = runCatching { cameraManager.cameraIdList.toList() }.getOrDefault(emptyList())
        val refreshRates = runCatching { display.supportedModes.map { it.refreshRate }.distinct().sorted() }
            .getOrDefault(listOf(mode.refreshRate))
        val hasFlash = cameraIds.any { id ->
            runCatching {
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }.getOrDefault(false)
        }
        val packageManager = context.packageManager
        return DeviceInfoSnapshot(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            model = Build.MODEL.orEmpty(),
            product = Build.PRODUCT.orEmpty(),
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty(),
            fingerprint = Build.FINGERPRINT.orEmpty(),
            cpuAbis = Build.SUPPORTED_ABIS.toList(),
            totalRamBytes = memory.totalMem,
            availableRamBytes = memory.availMem,
            totalStorageBytes = storage.totalBytes,
            availableStorageBytes = storage.availableBytes,
            screenWidthPixels = mode.physicalWidth,
            screenHeightPixels = mode.physicalHeight,
            densityDpi = metrics.densityDpi.takeIf { it != DisplayMetrics.DENSITY_DEFAULT } ?: metrics.densityDpi,
            currentRefreshRate = mode.refreshRate,
            availableRefreshRates = refreshRates,
            cameraCount = cameraIds.size,
            cellularAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY),
            bluetoothAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
            wifiAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI),
            nfcAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC),
            flashlightAvailable = hasFlash,
            sensorCount = context.getSystemService(SensorManager::class.java)
                .getSensorList(android.hardware.Sensor.TYPE_ALL).size,
        )
    }
}
