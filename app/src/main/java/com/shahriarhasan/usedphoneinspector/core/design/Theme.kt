package com.shahriarhasan.usedphoneinspector.core.design

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus
import com.shahriarhasan.usedphoneinspector.core.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF00677D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA9EDFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF4C626A),
    surface = Color(0xFFF7FAFC),
    surfaceVariant = Color(0xFFDCE4E8),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF55D6F5),
    onPrimary = Color(0xFF003640),
    primaryContainer = Color(0xFF004E5E),
    onPrimaryContainer = Color(0xFFA9EDFF),
    secondary = Color(0xFFB4CBD3),
    surface = Color(0xFF101416),
    surfaceVariant = Color(0xFF3F484C),
    error = Color(0xFFFFB4AB),
)

object StatusColors {
    val Pass = Color(0xFF16815D)
    val Warning = Color(0xFFA15C00)
    val Fail = Color(0xFFB3261E)
    val Neutral = Color(0xFF686F73)
    val Progress = Color(0xFF00677D)
}

fun statusColor(status: TestStatus): Color = when (status) {
    TestStatus.PASS -> StatusColors.Pass
    TestStatus.WARNING -> StatusColors.Warning
    TestStatus.FAIL -> StatusColors.Fail
    TestStatus.IN_PROGRESS -> StatusColors.Progress
    else -> StatusColors.Neutral
}

@Composable
fun InspectorTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}

