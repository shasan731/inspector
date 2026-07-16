package com.shahriarhasan.usedphoneinspector.feature.multitouch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R

@Composable
fun MultitouchScreen(onMetrics: (maximum: Int, coverage: Int) -> Unit) {
    val active = remember { mutableStateMapOf<PointerId, Offset>() }
    val trails = remember { mutableStateMapOf<PointerId, MutableList<Offset>>() }
    val touchedCells = remember { mutableStateMapOf<Int, Boolean>() }
    var maximum by remember { mutableIntStateOf(0) }
    var lastEvent by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.multitouch_instruction))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.current_touches, active.size))
            Text(stringResource(R.string.maximum_touches, maximum))
        }
        Text(stringResource(R.string.coverage_label, touchedCells.size * 100 / GRID_COUNT))
        if (lastEvent.isNotBlank()) Text(stringResource(R.string.last_touch_event, lastEvent))
        TouchCanvas(
            modifier = Modifier.fillMaxWidth().height(430.dp),
            active = active,
            trails = trails,
            touchedCells = touchedCells,
            onEvent = { event, max ->
                lastEvent = event
                maximum = max.coerceAtLeast(maximum)
                onMetrics(maximum, touchedCells.size * 100 / GRID_COUNT)
            },
        )
        OutlinedButton(
            onClick = {
                active.clear()
                trails.clear()
                touchedCells.clear()
                maximum = 0
                lastEvent = ""
                onMetrics(0, 0)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.reset)) }
    }
}

@Composable
private fun TouchCanvas(
    modifier: Modifier,
    active: MutableMap<PointerId, Offset>,
    trails: MutableMap<PointerId, MutableList<Offset>>,
    touchedCells: MutableMap<Int, Boolean>,
    onEvent: (String, Int) -> Unit,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val trailColor = MaterialTheme.colorScheme.primary
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            val id = change.id
                            if (change.pressed) {
                                val isNew = id !in active
                                active[id] = change.position
                                trails.getOrPut(id) { mutableListOf() }.add(change.position)
                                val column = (change.position.x / (size.width / GRID_COLUMNS)).toInt().coerceIn(0, GRID_COLUMNS - 1)
                                val row = (change.position.y / (size.height / GRID_ROWS)).toInt().coerceIn(0, GRID_ROWS - 1)
                                touchedCells[row * GRID_COLUMNS + column] = true
                                if (isNew) onEvent("DOWN ${id.value}: ${change.position.x.toInt()}, ${change.position.y.toInt()}", active.size)
                                change.consume()
                            } else if (id in active) {
                                active.remove(id)
                                onEvent("UP ${id.value}", active.size)
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                }
            },
        ) {
            val cellWidth = size.width / GRID_COLUMNS
            val cellHeight = size.height / GRID_ROWS
            repeat(GRID_COLUMNS + 1) { x ->
                drawLine(gridColor, Offset(x * cellWidth, 0f), Offset(x * cellWidth, size.height), strokeWidth = 1f)
            }
            repeat(GRID_ROWS + 1) { y ->
                drawLine(gridColor, Offset(0f, y * cellHeight), Offset(size.width, y * cellHeight), strokeWidth = 1f)
            }
            touchedCells.keys.forEach { cell ->
                val x = (cell % GRID_COLUMNS) * cellWidth
                val y = (cell / GRID_COLUMNS) * cellHeight
                drawRect(trailColor.copy(alpha = 0.15f), Offset(x, y), androidx.compose.ui.geometry.Size(cellWidth, cellHeight))
            }
            trails.values.forEach { points ->
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, trailColor, style = Stroke(width = 5f))
                }
            }
            active.forEach { (id, position) ->
                drawCircle(Color.White, radius = 30f, center = position)
                drawCircle(trailColor, radius = 30f, center = position, style = Stroke(width = 5f))
                drawContext.canvas.nativeCanvas.drawText(
                    id.value.toString(),
                    position.x - 8f,
                    position.y + 8f,
                    android.graphics.Paint().apply { color = android.graphics.Color.BLACK; textSize = 24f },
                )
            }
        }
    }
}

private const val GRID_COLUMNS = 8
private const val GRID_ROWS = 12
private const val GRID_COUNT = GRID_COLUMNS * GRID_ROWS
