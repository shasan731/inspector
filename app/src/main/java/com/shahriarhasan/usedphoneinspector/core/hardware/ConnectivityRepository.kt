package com.shahriarhasan.usedphoneinspector.core.hardware

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class WifiSnapshot(
    val available: Boolean,
    val enabled: Boolean,
    val connected: Boolean,
    val validated: Boolean,
    val ssid: String?,
    val signalLevel: Int?,
    val linkSpeedMbps: Int?,
    val frequencyMhz: Int?,
    val transport: String?,
)

data class BluetoothSnapshot(
    val available: Boolean,
    val enabled: Boolean,
    val adapterName: String?,
    val pairedCount: Int?,
)

sealed interface BluetoothScanState {
    data object Idle : BluetoothScanState
    data class Scanning(val discoveredCount: Int) : BluetoothScanState
    data class Finished(val discoveredCount: Int) : BluetoothScanState
    data class Error(val reason: String) : BluetoothScanState
}

interface ConnectivityRepository {
    fun wifiSnapshot(): WifiSnapshot
    fun bluetoothSnapshot(): BluetoothSnapshot
    fun scanBluetooth(): Flow<BluetoothScanState>
    fun cancelBluetoothScan()
}

@Singleton
class AndroidConnectivityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectivityRepository {
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)

    override fun wifiSnapshot(): WifiSnapshot {
        val packageManager = context.packageManager
        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
        val wifiInfo = capabilities?.transportInfo as? WifiInfo
        return WifiSnapshot(
            available = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI),
            enabled = wifiManager.isWifiEnabled,
            connected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
            validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            ssid = runCatching { wifiInfo?.ssid?.takeUnless { it == WifiManager.UNKNOWN_SSID } }.getOrNull(),
            signalLevel = wifiInfo?.let { WifiManager.calculateSignalLevel(it.rssi, 5) },
            linkSpeedMbps = wifiInfo?.linkSpeed,
            frequencyMhz = wifiInfo?.frequency,
            transport = when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "CELLULAR"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "ETHERNET"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN"
                else -> null
            },
        )
    }

    override fun bluetoothSnapshot(): BluetoothSnapshot {
        val adapter = bluetoothManager.adapter
        val canConnect = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        return BluetoothSnapshot(
            available = adapter != null,
            enabled = runCatching { adapter?.isEnabled == true }.getOrDefault(false),
            adapterName = if (canConnect) runCatching { adapter?.name }.getOrNull() else null,
            pairedCount = if (canConnect) runCatching { adapter?.bondedDevices?.size }.getOrNull() else null,
        )
    }

    override fun scanBluetooth(): Flow<BluetoothScanState> = callbackFlow {
        val adapter = bluetoothManager.adapter
        if (adapter == null) {
            trySend(BluetoothScanState.Error("UNSUPPORTED"))
            close()
            return@callbackFlow
        }
        val found = mutableSetOf<String>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= 33) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        device?.address?.let(found::add)
                        trySend(BluetoothScanState.Scanning(found.size))
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> trySend(BluetoothScanState.Finished(found.size))
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        try {
            if (adapter.startDiscovery()) trySend(BluetoothScanState.Scanning(0))
            else trySend(BluetoothScanState.Error("START_FAILED"))
        } catch (_: SecurityException) {
            trySend(BluetoothScanState.Error("PERMISSION_DENIED"))
        }
        awaitClose {
            runCatching { adapter.cancelDiscovery() }
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    override fun cancelBluetoothScan() {
        runCatching { bluetoothManager.adapter?.cancelDiscovery() }
    }
}

