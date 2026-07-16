package com.shahriarhasan.usedphoneinspector.core.hardware

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface VibrationRepository {
    val hasVibrator: Boolean
    fun shortPulse()
    fun longPulse()
    fun pattern()
    fun stop()
}

class AndroidVibrationRepository @Inject constructor(
    @ApplicationContext context: Context,
) : VibrationRepository {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
    }
    override val hasVibrator: Boolean get() = vibrator.hasVibrator()
    override fun shortPulse() = vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    override fun longPulse() = vibrate(VibrationEffect.createOneShot(1_000, VibrationEffect.DEFAULT_AMPLITUDE))
    override fun pattern() = vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 300, 100, 150), 0))
    override fun stop() = vibrator.cancel()
    private fun vibrate(effect: VibrationEffect) { if (hasVibrator) vibrator.vibrate(effect) }
}

