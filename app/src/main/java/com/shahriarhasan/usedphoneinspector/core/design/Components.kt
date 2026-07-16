package com.shahriarhasan.usedphoneinspector.core.design

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus

@Composable
fun TechnicalRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun StatusChip(status: TestStatus) {
    val (label, icon) = statusLabelIcon(status)
    AssistChip(
        onClick = {},
        label = { Text(stringResource(label)) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = statusColor(status))
        },
    )
}

@Composable
fun InformationCard(title: String, body: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun statusLabelIcon(status: TestStatus): Pair<Int, ImageVector> = when (status) {
    TestStatus.NOT_STARTED -> R.string.status_not_started to Icons.Default.PlayCircle
    TestStatus.IN_PROGRESS -> R.string.status_in_progress to Icons.Default.HourglassTop
    TestStatus.PASS -> R.string.status_pass to Icons.Default.CheckCircle
    TestStatus.WARNING -> R.string.status_warning to Icons.Default.ReportProblem
    TestStatus.FAIL -> R.string.status_fail to Icons.Default.Cancel
    TestStatus.SKIPPED -> R.string.status_skipped to Icons.Default.SkipNext
    TestStatus.UNSUPPORTED -> R.string.status_unsupported to Icons.Default.Block
    TestStatus.PERMISSION_DENIED -> R.string.status_permission_denied to Icons.Default.Lock
}

