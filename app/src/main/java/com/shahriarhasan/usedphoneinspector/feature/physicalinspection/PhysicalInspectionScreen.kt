package com.shahriarhasan.usedphoneinspector.feature.physicalinspection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.database.PhysicalCheckEntity
import com.shahriarhasan.usedphoneinspector.core.model.PhysicalChecklist
import com.shahriarhasan.usedphoneinspector.core.model.PhysicalCondition

data class PhysicalDraft(val categoryKey: String, val condition: PhysicalCondition, val notes: String = "")

@Composable
fun PhysicalInspectionScreen(
    existing: List<PhysicalCheckEntity>,
    onChecksChanged: (Map<String, PhysicalDraft>) -> Unit,
) {
    val values = remember(existing) {
        mutableStateMapOf<String, PhysicalDraft>().apply {
            PhysicalChecklist.categories.forEach { category ->
                category.items.forEach { item ->
                    val saved = existing.firstOrNull { it.itemKey == item.key }
                    put(item.key, PhysicalDraft(category.categoryKey, saved?.condition ?: PhysicalCondition.NOT_CHECKED, saved?.notes.orEmpty()))
                }
            }
        }
    }
    var customLabel by remember { mutableStateOf("") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(stringResource(R.string.physical_instruction)) }
        PhysicalChecklist.categories.forEach { category ->
            item { Text(stringResource(category.categoryLabel), style = MaterialTheme.typography.titleMedium) }
            category.items.forEach { item ->
                item(key = item.key) {
                    val value = requireNotNull(values[item.key])
                    PhysicalItemRow(
                        label = stringResource(item.label),
                        value = value,
                        onChange = {
                            values[item.key] = it
                            onChecksChanged(values.toMap())
                        },
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it.take(100) },
                    label = { Text(stringResource(R.string.custom_item_label)) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    if (customLabel.isNotBlank()) {
                        val key = "custom_${customLabel.trim().lowercase().hashCode()}"
                        values[key] = PhysicalDraft("custom", PhysicalCondition.NOT_CHECKED, customLabel.trim())
                        onChecksChanged(values.toMap())
                        customLabel = ""
                    }
                }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_custom_item)) }
            }
        }
    }
}

@Composable
private fun PhysicalItemRow(label: String, value: PhysicalDraft, onChange: (PhysicalDraft) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, modifier = Modifier.weight(1f))
                Box {
                    Button(onClick = { menu = true }) {
                        Text(conditionLabel(value.condition))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        PhysicalCondition.entries.forEach { condition ->
                            DropdownMenuItem(
                                text = { Text(conditionLabel(condition)) },
                                onClick = { menu = false; onChange(value.copy(condition = condition)) },
                            )
                        }
                    }
                }
            }
            if (expanded) {
                OutlinedTextField(
                    value = value.notes,
                    onValueChange = { onChange(value.copy(notes = it.take(500))) },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun conditionLabel(condition: PhysicalCondition): String = stringResource(
    when (condition) {
        PhysicalCondition.GOOD -> R.string.condition_good
        PhysicalCondition.MINOR_ISSUE -> R.string.condition_minor
        PhysicalCondition.MAJOR_ISSUE -> R.string.condition_major
        PhysicalCondition.NOT_CHECKED -> R.string.condition_not_checked
        PhysicalCondition.NOT_APPLICABLE -> R.string.condition_not_applicable
    },
)

