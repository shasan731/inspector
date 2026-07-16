package com.shahriarhasan.usedphoneinspector.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.database.InspectionWithDetails
import com.shahriarhasan.usedphoneinspector.core.design.StatusChip
import com.shahriarhasan.usedphoneinspector.core.model.ConditionGrade
import com.shahriarhasan.usedphoneinspector.core.model.HistoryFilter
import com.shahriarhasan.usedphoneinspector.core.model.HistorySort
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import com.shahriarhasan.usedphoneinspector.core.model.InspectionStatus
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    results: List<InspectionWithDetails>,
    filter: HistoryFilter,
    viewModel: HistoryViewModel,
    onOpen: (String) -> Unit,
    onResume: (String) -> Unit,
) {
    var deleteId by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium) }
        item {
            OutlinedTextField(
                value = filter.query,
                onValueChange = viewModel::setQuery,
                label = { Text(stringResource(R.string.search_history)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterMenu(
                    current = filter.profile?.let { stringResource(it.labelRes) } ?: stringResource(R.string.all_profiles),
                    items = listOf(stringResource(R.string.all_profiles) to { viewModel.setProfile(null) }) +
                        InspectionProfile.entries.map { profile -> stringResource(profile.labelRes) to { viewModel.setProfile(profile) } },
                    modifier = Modifier.weight(1f),
                )
                FilterMenu(
                    current = filter.grade?.let { stringResource(it.labelRes) } ?: stringResource(R.string.all_grades),
                    items = listOf(stringResource(R.string.all_grades) to { viewModel.setGrade(null) }) +
                        ConditionGrade.entries.map { grade -> stringResource(grade.labelRes) to { viewModel.setGrade(grade) } },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterMenu(
                    current = filter.status?.name ?: stringResource(R.string.all_statuses),
                    items = listOf(stringResource(R.string.all_statuses) to { viewModel.setStatus(null) }) +
                        InspectionStatus.entries.map { status -> status.name to { viewModel.setStatus(status) } },
                    modifier = Modifier.weight(1f),
                )
                val sortLabels = mapOf(
                    HistorySort.NEWEST to R.string.sort_newest,
                    HistorySort.OLDEST to R.string.sort_oldest,
                    HistorySort.HIGHEST_SCORE to R.string.sort_highest,
                    HistorySort.LOWEST_SCORE to R.string.sort_lowest,
                )
                FilterMenu(
                    current = stringResource(requireNotNull(sortLabels[filter.sort])),
                    items = HistorySort.entries.map { sort -> stringResource(requireNotNull(sortLabels[sort])) to { viewModel.setSort(sort) } },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (results.isEmpty()) item { Text(stringResource(R.string.no_history_results)) }
        items(results, key = { it.inspection.id }) { details ->
            HistoryCard(details, onOpen, onResume, viewModel::duplicate) { deleteId = details.inspection.id }
        }
    }
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text(stringResource(R.string.delete_inspection_title)) },
            text = { Text(stringResource(R.string.delete_inspection_body)) },
            confirmButton = {
                TextButton(onClick = { deleteId?.let(viewModel::delete); deleteId = null }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun HistoryCard(
    details: InspectionWithDetails,
    onOpen: (String) -> Unit,
    onResume: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val inspection = details.inspection
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${inspection.brand} ${inspection.model}", style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(inspection.profile.labelRes))
                    Text(DateFormat.getDateInstance().format(Date(inspection.updatedAt)))
                    details.sellers.firstOrNull()?.name?.takeIf(String::isNotBlank)?.let { Text(it) }
                    inspection.conditionScore?.let {
                        Text(stringResource(R.string.score_format, it) + " • " + stringResource(R.string.coverage_format, inspection.coveragePercent ?: 0))
                    }
                }
                StatusChip(if (inspection.status == InspectionStatus.COMPLETED) TestStatus.PASS else TestStatus.IN_PROGRESS)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { onOpen(inspection.id) }) { Icon(Icons.Default.Visibility, stringResource(R.string.view_report)) }
                if (inspection.status != InspectionStatus.COMPLETED) {
                    IconButton(onClick = { onResume(inspection.id) }) { Icon(Icons.Default.Edit, stringResource(R.string.resume_inspection)) }
                }
                IconButton(onClick = { onDuplicate(inspection.id) }) { Icon(Icons.Default.ContentCopy, stringResource(R.string.duplicate)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
            }
        }
    }
}

@Composable
private fun FilterMenu(current: String, items: List<Pair<String, () -> Unit>>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(current, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            items.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; action() })
            }
        }
    }
}

