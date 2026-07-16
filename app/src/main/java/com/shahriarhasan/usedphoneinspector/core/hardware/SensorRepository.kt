package com.shahriarhasan.usedphoneinspector.core.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class SensorDescriptor(
    val name: String,
    val type: Int,
    val vendor: String,
    val version: Int,
    val maximumRange: Float,
    val resolution: Float,
    val powerMilliamp: Float,
    val minimumDelayMicroseconds: Int,
    val reportingMode: Int,
    val isWakeUpSensor: Boolean,
)

data class SensorReading(val type: Int, val values: List<Float>, val accuracy: Int, val timestampNanos: Long)

interface SensorRepository {
    fun sensors(): List<SensorDescriptor>
    fun observe(type: Int): Flow<SensorReading>
}

@Singleton
class AndroidSensorRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SensorRepository {
    private val manager = context.getSystemService(SensorManager::class.java)

    override fun sensors(): List<SensorDescriptor> = manager.getSensorList(Sensor.TYPE_ALL).map { sensor ->
        SensorDescriptor(
            name = sensor.name,
            type = sensor.type,
            vendor = sensor.vendor,
            version = sensor.version,
            maximumRange = sensor.maximumRange,
            resolution = sensor.resolution,
            powerMilliamp = sensor.power,
            minimumDelayMicroseconds = sensor.minDelay,
            reportingMode = sensor.reportingMode,
            isWakeUpSensor = sensor.isWakeUpSensor,
        )
    }

    override fun observe(type: Int): Flow<SensorReading> = callbackFlow {
        val sensor = manager.getDefaultSensor(type)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(SensorReading(event.sensor.type, event.values.toList(), event.accuracy, event.timestamp))
            }
            override fun onAccuracyChanged(changedSensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { manager.unregisterListener(listener, sensor) }
    }
}

