package com.shahriarhasan.usedphoneinspector.core.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BatterySnapshot(
    val levelPercent: Int?,
    val isCharging: Boolean,
    val pluggedType: Int,
    val status: Int,
    val health: Int,
    val temperatureCelsius: Float?,
    val voltageMillivolts: Int?,
    val currentNowMicroamps: Int?,
    val currentAverageMicroamps: Int?,
    val chargeCounterMicroampHours: Int?,
    val technology: String?,
    val powerSaveMode: Boolean,
    val chargeTimeRemainingMillis: Long?,
    val stateChangedAt: Long,
) {
    val estimatedWatts: Double?
        get() = if (isCharging && voltageMillivolts != null && currentNowMicroamps != null) {
            kotlin.math.abs(voltageMillivolts.toDouble() * currentNowMicroamps.toDouble()) / 1_000_000_000.0
        } else null
}

interface BatteryRepository {
    fun observe(): Flow<BatterySnapshot>
}

@Singleton
class AndroidBatteryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : BatteryRepository {
    override fun observe(): Flow<BatterySnapshot> = callbackFlow {
        val manager = context.getSystemService(BatteryManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)
        var changedAt = System.currentTimeMillis()
        var previousStatus: Int? = null

        fun send(intent: Intent) {
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            if (previousStatus != status) changedAt = System.currentTimeMillis()
            previousStatus = status
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = if (level >= 0 && scale > 0) level * 100 / scale else null
            trySend(
                BatterySnapshot(
                    levelPercent = percent,
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL,
                    pluggedType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
                    status = status,
                    health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
                    temperatureCelsius = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                        .takeIf { it != Int.MIN_VALUE }?.div(10f),
                    voltageMillivolts = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Int.MIN_VALUE)
                        .takeIf { it != Int.MIN_VALUE },
                    currentNowMicroamps = manager.intProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
                    currentAverageMicroamps = manager.intProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE),
                    chargeCounterMicroampHours = manager.intProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
                    technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY),
                    powerSaveMode = powerManager.isPowerSaveMode,
                    chargeTimeRemainingMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        manager.computeChargeTimeRemaining().takeIf { it >= 0 }
                    } else null,
                    stateChangedAt = changedAt,
                ),
            )
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent != null) send(intent)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )?.let(::send)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun BatteryManager.intProperty(id: Int): Int? =
        getIntProperty(id).takeIf { it != Int.MIN_VALUE }
}
