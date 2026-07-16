package com.shahriarhasan.usedphoneinspector.feature.displaytest

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.design.InformationCard
import com.shahriarhasan.usedphoneinspector.core.utilities.findActivity

private data class TestColour(val label: Int, val color: Color?, val gradient: Brush? = null)

@Composable
fun DisplayTestScreen(issues: Set<String>, onIssuesChanged: (Set<String>) -> Unit) {
    var fullscreen by remember { mutableStateOf(false) }
    var raiseBrightness by remember { mutableStateOf(false) }
    val issueOptions = listOf(
        "dead_pixels" to R.string.issue_dead_pixels,
        "burn_in" to R.string.issue_burn_in,
        "discoloration" to R.string.issue_discoloration,
        "light_bleed" to R.string.issue_light_bleed,
        "lines" to R.string.issue_lines,
        "flicker" to R.string.issue_flicker,
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InformationCard(stringResource(R.string.display_instruction_title), stringResource(R.string.display_instruction_body))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.raise_brightness))
            Switch(checked = raiseBrightness, onCheckedChange = { raiseBrightness = it })
        }
        Button(onClick = { fullscreen = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.begin_display_test))
        }
        Text(stringResource(R.string.manual_observation_required), style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(issueOptions.size) { index ->
                val (key, label) = issueOptions[index]
                FilterChip(
                    selected = key in issues,
                    onClick = {
                        onIssuesChanged(if (key in issues) issues - key else issues + key)
                    },
                    label = { Text(stringResource(label)) },
                )
            }
        }
    }
    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            DisplayColourPager(raiseBrightness = raiseBrightness, onExit = { fullscreen = false })
        }
    }
}

@Composable
private fun DisplayColourPager(raiseBrightness: Boolean, onExit: () -> Unit) {
    val colours = listOf(
        TestColour(R.string.colour_red, Color.Red),
        TestColour(R.string.colour_green, Color.Green),
        TestColour(R.string.colour_blue, Color.Blue),
        TestColour(R.string.colour_white, Color.White),
        TestColour(R.string.colour_black, Color.Black),
        TestColour(R.string.colour_grey, Color(0xFF808080)),
        TestColour(R.string.colour_gradient, null, Brush.horizontalGradient(listOf(Color.Black, Color.White))),
    )
    var page by remember { mutableIntStateOf(0) }
    val item = colours[page]
    FullScreenTestEffect(raiseBrightness)
    BackHandler(onBack = onExit)
    Box(
        Modifier.fillMaxSize()
            .then(if (item.gradient != null) Modifier.background(item.gradient) else Modifier.background(requireNotNull(item.color)))
            .pointerInput(page) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -20) page = (page + 1).coerceAtMost(colours.lastIndex)
                    if (dragAmount > 20) page = (page - 1).coerceAtLeast(0)
                }
            },
    ) {
        TextButton(
            onClick = onExit,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 8.dp),
        ) {
            Text(stringResource(R.string.exit_fullscreen), color = if (page == 3) Color.Black else Color.White)
        }
        Text(
            stringResource(item.label),
            color = if (page == 3 || page == 5) Color.Black else Color.White,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
        )
    }
}

@Composable
private fun FullScreenTestEffect(raiseBrightness: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, raiseBrightness) {
        val window = activity?.window
        val previousBrightness = window?.attributes?.screenBrightness
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (raiseBrightness) {
                window.attributes = window.attributes.apply { screenBrightness = 1f }
            }
        }
        onDispose {
            if (window != null) {
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window.attributes = window.attributes.apply {
                    screenBrightness = previousBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }
}

