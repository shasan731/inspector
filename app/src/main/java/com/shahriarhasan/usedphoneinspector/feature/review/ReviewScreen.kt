package com.shahriarhasan.usedphoneinspector.feature.review

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.design.StatusChip
import com.shahriarhasan.usedphoneinspector.core.model.InspectionStatus
import com.shahriarhasan.usedphoneinspector.core.utilities.findActivity

@Composable
fun ReviewScreen(state: ReviewUiState, viewModel: ReviewViewModel, onEdit: () -> Unit, onUpgrade: () -> Unit) {
    val context = LocalContext.current
    val details = state.details ?: return
    val score = state.score ?: return
    var scoreExpanded by remember { mutableStateOf(false) }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val report = state.report
        if (uri != null && report != null) runCatching { viewModel.reportActions.saveTo(context, report, uri) }
    }
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(R.string.review_title), style = MaterialTheme.typography.headlineMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${details.inspection.brand} ${details.inspection.model}", style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(details.inspection.profile.labelRes))
                    details.inspection.reportId?.let { Text(stringResource(R.string.report_id) + ": " + it) }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().clickable { scoreExpanded = !scoreExpanded }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.condition_score), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.score_format, score.score), style = MaterialTheme.typography.headlineSmall)
                    }
                    Text(stringResource(R.string.inspection_coverage) + ": " + stringResource(R.string.coverage_format, score.coveragePercent))
                    Text(stringResource(score.grade.labelRes))
                    if (score.coveragePercent < 60) Text(stringResource(R.string.insufficient_coverage), color = MaterialTheme.colorScheme.error)
                    Row { Icon(Icons.Default.ExpandMore, contentDescription = null); Text(stringResource(R.string.how_score_calculated)) }
                    if (scoreExpanded) {
                        Text(stringResource(R.string.score_explanation))
                        score.categoryScores.forEach { (category, value) ->
                            Text(stringResource(category.labelRes) + ": " + stringResource(R.string.score_format, value))
                        }
                    }
                }
            }
        }
        item { Text(stringResource(R.string.review_results), style = MaterialTheme.typography.titleLarge) }
        items(details.testResults, key = { it.id }) { result ->
            val title = com.shahriarhasan.usedphoneinspector.core.model.InspectionProfiles.allTests
                .firstOrNull { it.id == result.testId }?.titleRes
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title?.let { stringResource(it) } ?: result.testId, modifier = Modifier.weight(1f))
                    StatusChip(result.status)
                }
            }
        }
        item {
            if (details.inspection.status == InspectionStatus.COMPLETED) {
                OutlinedButton(onClick = { viewModel.reopen(); onEdit() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.edit_inspection))
                }
                Button(onClick = viewModel::generateReport, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    if (state.busy) CircularProgressIndicator() else Text(stringResource(R.string.generate_report))
                }
            } else {
                Button(onClick = viewModel::complete, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    if (state.busy) CircularProgressIndicator() else Text(stringResource(R.string.complete_inspection))
                }
            }
        }
        state.report?.let { report ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", report.file)
                            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.view_report)) }
                    OutlinedButton(onClick = { viewModel.reportActions.share(context, report) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.share))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { saveLauncher.launch(report.filename) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.save))
                    }
                    OutlinedButton(
                        onClick = { context.findActivity()?.let { viewModel.reportActions.print(it, report) } },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.print)) }
                }
            }
        }
        if (state.reportError) item { Text(stringResource(R.string.report_error), color = MaterialTheme.colorScheme.error) }
    }
    if (state.completionLimitReached) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLimit,
            title = { Text(stringResource(R.string.completion_limit_title)) },
            text = { Text(stringResource(R.string.completion_limit_body)) },
            confirmButton = { TextButton(onClick = onUpgrade) { Text(stringResource(R.string.upgrade)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissLimit) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

