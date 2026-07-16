package com.shahriarhasan.usedphoneinspector.core.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class TelephonySnapshot(
    val available: Boolean,
    val simSlotCount: Int,
    val simStates: List<Int>,
    val carrierName: String?,
    val phoneType: Int?,
    val dataNetworkType: Int?,
    val roaming: Boolean?,
    val airplaneMode: Boolean,
    val permissionGranted: Boolean,
)

interface TelephonyRepository { fun snapshot(): TelephonySnapshot }

@Singleton
class AndroidTelephonyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : TelephonyRepository {
    override fun snapshot(): TelephonySnapshot {
        val available = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        val manager = context.getSystemService(TelephonyManager::class.java)
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
        val slots = if (available) {
            val modemCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.activeModemCount
            } else {
                @Suppress("DEPRECATION")
                manager.phoneCount
            }
            modemCount.coerceAtLeast(1)
        } else {
            0
        }
        return TelephonySnapshot(
            available = available,
            simSlotCount = slots,
            simStates = (0 until slots).map { slot -> runCatching { manager.getSimState(slot) }.getOrDefault(TelephonyManager.SIM_STATE_UNKNOWN) },
            carrierName = runCatching { manager.networkOperatorName.takeIf(String::isNotBlank) }.getOrNull(),
            phoneType = if (available) manager.phoneType else null,
            dataNetworkType = if (permission) runCatching { manager.dataNetworkType }.getOrNull() else null,
            roaming = if (permission) runCatching { manager.isNetworkRoaming }.getOrNull() else null,
            airplaneMode = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0,
            permissionGranted = permission,
        )
    }
}
