package com.shahriarhasan.usedphoneinspector.feature.inspection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.model.DeviceCategory
import com.shahriarhasan.usedphoneinspector.core.model.InspectionDraft
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile

@Composable
fun InspectionSetupScreen(state: SetupUiState, onEvent: (SetupEvent) -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Text(stringResource(R.string.setup_intro))
                }
            }
        }
        item {
            EnumMenu(
                label = stringResource(R.string.inspection_profile),
                value = stringResource(state.draft.profile.labelRes),
                entries = InspectionProfile.entries.map { stringResource(it.labelRes) to { onEvent(SetupEvent.ProfileChanged(it)) } },
            )
        }
        item {
            EnumMenu(
                label = stringResource(R.string.device_category),
                value = stringResource(state.draft.deviceCategory.labelRes),
                entries = DeviceCategory.entries.map { stringResource(it.labelRes) to { onEvent(SetupEvent.CategoryChanged(it)) } },
            )
        }
        identityFields(state.draft, onEvent)
        item {
            Text(stringResource(R.string.imei_unverified), style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.open_device_settings)) }
                OutlinedButton(
                    onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)) }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.open_about_phone)) }
            }
        }
        if (state.showRequiredError) {
            item { Text(stringResource(R.string.required_identity_error), color = MaterialTheme.colorScheme.error) }
        }
        item {
            Button(
                onClick = { onEvent(SetupEvent.Submit) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            ) {
                if (state.isSaving) CircularProgressIndicator() else Text(stringResource(R.string.start_inspection))
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.identityFields(
    draft: InspectionDraft,
    onEvent: (SetupEvent) -> Unit,
) {
    item { FormField(R.string.brand, draft.brand) { onEvent(SetupEvent.BrandChanged(it)) } }
    item { FormField(R.string.model, draft.model) { onEvent(SetupEvent.ModelChanged(it)) } }
    item { FormField(R.string.device_color, draft.color) { onEvent(SetupEvent.ColorChanged(it)) } }
    item { FormField(R.string.storage_variant, draft.storageVariant) { onEvent(SetupEvent.StorageChanged(it)) } }
    item { FormField(R.string.ram_variant, draft.ramVariant) { onEvent(SetupEvent.RamChanged(it)) } }
    item { FormField(R.string.asking_price, draft.askingPrice, KeyboardType.Decimal) { onEvent(SetupEvent.AskingPriceChanged(it)) } }
    item { FormField(R.string.agreed_price, draft.finalPrice, KeyboardType.Decimal) { onEvent(SetupEvent.FinalPriceChanged(it)) } }
    item { FormField(R.string.currency, draft.currency) { onEvent(SetupEvent.CurrencyChanged(it)) } }
    item { FormField(R.string.purchase_source, draft.purchaseSource) { onEvent(SetupEvent.SourceChanged(it)) } }
    item { FormField(R.string.serial_number, draft.serialNumber) { onEvent(SetupEvent.SerialChanged(it)) } }
    item { ImeiField(R.string.imei_one, draft.imei1) { onEvent(SetupEvent.Imei1Changed(it)) } }
    item { ImeiField(R.string.imei_two, draft.imei2) { onEvent(SetupEvent.Imei2Changed(it)) } }
    item { FormField(R.string.notes, draft.notes, singleLine = false) { onEvent(SetupEvent.NotesChanged(it)) } }
}

@Composable
private fun FormField(
    label: Int,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ImeiField(label: Int, value: String, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    val clipboardLabel = stringResource(label)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        trailingIcon = {
            Row {
                IconButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText(clipboardLabel, value))
                }) { Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy_imei)) }
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Info, contentDescription = stringResource(R.string.clear))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EnumMenu(label: String, value: String, entries: List<Pair<String, () -> Unit>>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { (text, action) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { expanded = false; action() },
                )
            }
        }
    }
}
