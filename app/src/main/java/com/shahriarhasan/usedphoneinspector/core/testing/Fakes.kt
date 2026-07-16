package com.shahriarhasan.usedphoneinspector.core.testing

import android.app.Activity
import android.hardware.Sensor
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.shahriarhasan.usedphoneinspector.core.billing.BillingConnectionState
import com.shahriarhasan.usedphoneinspector.core.billing.BillingRepository
import com.shahriarhasan.usedphoneinspector.core.billing.BillingUiState
import com.shahriarhasan.usedphoneinspector.core.hardware.BatteryRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.BatterySnapshot
import com.shahriarhasan.usedphoneinspector.core.hardware.BluetoothScanState
import com.shahriarhasan.usedphoneinspector.core.hardware.BluetoothSnapshot
import com.shahriarhasan.usedphoneinspector.core.hardware.CameraDescriptor
import com.shahriarhasan.usedphoneinspector.core.hardware.CameraRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.CameraTestController
import com.shahriarhasan.usedphoneinspector.core.hardware.CameraTestState
import com.shahriarhasan.usedphoneinspector.core.hardware.ConnectivityRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.DeviceInfoRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.DeviceInfoSnapshot
import com.shahriarhasan.usedphoneinspector.core.hardware.SensorDescriptor
import com.shahriarhasan.usedphoneinspector.core.hardware.SensorReading
import com.shahriarhasan.usedphoneinspector.core.hardware.SensorRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.TelephonyRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.TelephonySnapshot
import com.shahriarhasan.usedphoneinspector.core.hardware.WifiSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

class FakeBillingRepository(initiallyPro: Boolean = false) : BillingRepository {
    private val mutableState = MutableStateFlow(
        BillingUiState(connection = BillingConnectionState.READY, isPro = initiallyPro, localizedPrice = null),
    )
    override val state: StateFlow<BillingUiState> = mutableState
    override fun connect() = Unit
    override fun launchPurchase(activity: Activity) { mutableState.value = mutableState.value.copy(isPro = true) }
    override fun restorePurchases() = Unit
    override fun close() = Unit
}

class FakeBatteryRepository(
    snapshot: BatterySnapshot = BatterySnapshot(50, false, 0, 0, 0, 25f, 4_000, null, null, null, "Li-ion", false, null, 0),
) : BatteryRepository {
    private val value = MutableStateFlow(snapshot)
    override fun observe(): Flow<BatterySnapshot> = value
    fun emit(snapshot: BatterySnapshot) { value.value = snapshot }
}

class FakeSensorRepository(private val descriptors: List<SensorDescriptor> = emptyList()) : SensorRepository {
    private val readings = mutableMapOf<Int, MutableStateFlow<SensorReading>>()
    override fun sensors(): List<SensorDescriptor> = descriptors
    override fun observe(type: Int): Flow<SensorReading> = readings.getOrPut(type) {
        MutableStateFlow(SensorReading(type, listOf(0f, 0f, 0f), 3, 0))
    }
    fun emit(type: Int, values: List<Float>) {
        readings.getOrPut(type) { MutableStateFlow(SensorReading(type, values, 3, 0)) }.value = SensorReading(type, values, 3, 1)
    }
}

class FakeConnectivityRepository : ConnectivityRepository {
    var wifi = WifiSnapshot(true, true, true, true, "Test", 4, 300, 5_180, "WIFI")
    var bluetooth = BluetoothSnapshot(true, true, "Test", 1)
    override fun wifiSnapshot(): WifiSnapshot = wifi
    override fun bluetoothSnapshot(): BluetoothSnapshot = bluetooth
    override fun scanBluetooth(): Flow<BluetoothScanState> = flowOf(BluetoothScanState.Finished(2))
    override fun cancelBluetoothScan() = Unit
}

class FakeTelephonyRepository : TelephonyRepository {
    var value = TelephonySnapshot(true, 2, listOf(5, 5), "Carrier", 1, 13, false, false, true)
    override fun snapshot(): TelephonySnapshot = value
}

class FakeCameraRepository(private val supported: Boolean = true) : CameraRepository {
    override fun cameras(): List<CameraDescriptor> = if (supported) listOf(CameraDescriptor("0", 1, listOf(4f), "4000×3000", true, 3)) else emptyList()
    override fun setTorch(enabled: Boolean): Boolean = supported
    override fun turnOffTorch() = Unit
}

class FakeCameraTestController : CameraTestController {
    private val mutableState = MutableStateFlow(CameraTestState())
    override val state: StateFlow<CameraTestState> = mutableState
    override fun bind(owner: LifecycleOwner, previewView: PreviewView) { mutableState.value = mutableState.value.copy(isBound = true) }
    override fun switchCamera(owner: LifecycleOwner, previewView: PreviewView) = Unit
    override fun focus(previewView: PreviewView, x: Float, y: Float) = Unit
    override fun zoomBy(scale: Float) = Unit
    override fun capture() = Unit
    override fun setTorch(enabled: Boolean) { mutableState.value = mutableState.value.copy(torchEnabled = enabled) }
    override fun clearCapture() = Unit
    override fun release() = Unit
}

class FakeDeviceInfoRepository : DeviceInfoRepository {
    override fun snapshot() = DeviceInfoSnapshot(
        "Maker", "Brand", "Model", "Product", "16", 36, "2026-01-01", "fingerprint",
        listOf("arm64-v8a"), 8_000_000_000, 4_000_000_000, 128_000_000_000, 64_000_000_000,
        1080, 2400, 420, 120f, listOf(60f, 120f), 2, true, true, true, true, true, 8,
    )
}
