package com.shahriarhasan.usedphoneinspector.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.database.InspectionEntity
import com.shahriarhasan.usedphoneinspector.core.database.InspectionStats
import com.shahriarhasan.usedphoneinspector.core.design.StatusChip
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    state: HomeUiState,
    onNewInspection: () -> Unit,
    onResume: (String) -> Unit,
    onView: (String) -> Unit,
    onUpgrade: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.home_subtitle, state.deviceModel), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.offline_private), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Button(onClick = onNewInspection, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.nav_new_inspection), modifier = Modifier.padding(start = 8.dp))
            }
        }
        state.resume?.let { resumable ->
            item {
                Card(Modifier.fillMaxWidth().clickable { onResume(resumable.inspection.id) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text(stringResource(R.string.resume_inspection), style = MaterialTheme.typography.titleMedium)
                        }
                        Text(stringResource(R.string.resume_device, resumable.inspection.brand, resumable.inspection.model))
                        TextButton(onClick = { onResume(resumable.inspection.id) }) {
                            Text(stringResource(R.string.continue_action))
                        }
                    }
                }
            }
        }
        item { StatsCard(state.stats) }
        if (!state.billing.isPro) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.upgrade_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.upgrade_body), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onUpgrade) { Text(stringResource(R.string.upgrade)) }
                    }
                }
            }
        }
        item { Text(stringResource(R.string.recent_inspections), style = MaterialTheme.typography.titleLarge) }
        if (state.recent.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.no_inspections_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.no_inspections_body))
                    }
                }
            }
        } else {
            items(state.recent, key = InspectionEntity::id) { inspection ->
                InspectionCard(inspection, onView)
            }
        }
        item { Text(if (state.billing.isPro) stringResource(R.string.pro_plan) else stringResource(R.string.free_plan)) }
    }
}

@Composable
private fun StatsCard(stats: InspectionStats) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(R.string.total_inspections, stats.total.toString())
                Stat(R.string.average_score, stats.averageScore?.toInt()?.toString() ?: "—")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(R.string.passed_count, stats.passed.toString())
                Stat(R.string.warning_count, stats.warnings.toString())
                Stat(R.string.failed_count, stats.failed.toString())
            }
        }
    }
}

@Composable
private fun Stat(label: Int, value: String) {
    Column { Text(value, style = MaterialTheme.typography.titleLarge); Text(stringResource(label)) }
}

@Composable
private fun InspectionCard(inspection: InspectionEntity, onView: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onView(inspection.id) }) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${inspection.brand} ${inspection.model}", style = MaterialTheme.typography.titleMedium)
                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(inspection.updatedAt)))
                inspection.conditionScore?.let { Text(stringResource(R.string.score_format, it)) }
            }
            StatusChip(if (inspection.status.name == "COMPLETED") TestStatus.PASS else TestStatus.IN_PROGRESS)
        }
    }
}

