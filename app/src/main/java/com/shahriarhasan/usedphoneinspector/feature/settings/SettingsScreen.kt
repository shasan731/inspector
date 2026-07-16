package com.shahriarhasan.usedphoneinspector.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.database.BrandingProfileEntity
import com.shahriarhasan.usedphoneinspector.core.model.AppLanguage
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import com.shahriarhasan.usedphoneinspector.core.model.ThemeMode

@Composable
fun SettingsScreen(state: SettingsUiState, viewModel: SettingsViewModel, onUpgrade: () -> Unit) {
    val context = LocalContext.current
    var deleteAll by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }
    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.updateLogo(uri)
    }
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium) }
        item { SectionTitle(R.string.settings_appearance) }
        item {
            ChoiceRow(
                R.string.theme,
                ThemeMode.entries,
                state.settings.themeMode,
                { mode -> stringResource(when (mode) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                }) },
                viewModel::setTheme,
            )
        }
        item { SwitchRow(R.string.dynamic_colour, state.settings.dynamicColor, viewModel::setDynamicColor) }
        item {
            ChoiceRow(
                R.string.language,
                AppLanguage.entries,
                state.settings.language,
                { language -> stringResource(when (language) {
                    AppLanguage.SYSTEM -> R.string.language_system
                    AppLanguage.ENGLISH -> R.string.language_english
                    AppLanguage.BANGLA -> R.string.language_bangla
                }) },
                viewModel::setLanguage,
            )
        }
        item { SectionTitle(R.string.settings_inspection) }
        item {
            ChoiceRow(
                R.string.default_profile,
                InspectionProfile.entries,
                state.settings.defaultProfile,
                { stringResource(it.labelRes) },
                viewModel::setProfile,
            )
        }
        item {
            OutlinedTextField(
                value = state.settings.defaultCurrency,
                onValueChange = viewModel::setCurrency,
                label = { Text(stringResource(R.string.default_currency)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { SwitchRow(R.string.keep_screen_awake, state.settings.keepScreenAwake, viewModel::setKeepAwake) }
        item { SwitchRow(R.string.require_failure_notes, state.settings.requireFailureNotes, viewModel::setFailureNotes) }
        item { SwitchRow(R.string.confirm_skips, state.settings.confirmBeforeSkip, viewModel::setConfirmSkip) }
        item { SectionTitle(R.string.settings_reports) }
        item {
            SwitchRow(R.string.include_seller_report, state.settings.includeSeller) {
                viewModel.setReportPreferences(it, state.settings.includeImei, state.settings.includePhotos)
            }
        }
        item {
            SwitchRow(R.string.include_imei_report, state.settings.includeImei) {
                viewModel.setReportPreferences(state.settings.includeSeller, it, state.settings.includePhotos)
            }
        }
        item {
            SwitchRow(R.string.include_photos_report, state.settings.includePhotos) {
                viewModel.setReportPreferences(state.settings.includeSeller, state.settings.includeImei, it)
            }
        }
        item {
            Text(stringResource(R.string.pdf_image_quality) + ": ${state.settings.pdfImageQuality}%")
            Slider(
                value = state.settings.pdfImageQuality.toFloat(),
                onValueChange = { viewModel.setImageQuality(it.toInt()) },
                valueRange = 40f..95f,
            )
        }
        item {
            OutlinedTextField(
                value = state.settings.reportFooter,
                onValueChange = viewModel::setFooter,
                label = { Text(stringResource(R.string.report_footer)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { SectionTitle(R.string.settings_branding) }
        if (state.billing.isPro) {
            brandingFields(state.branding, viewModel::updateBranding)
            item {
                OutlinedButton(onClick = { logoLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.evidence_photo))
                }
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth().clickable(onClick = onUpgrade)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        androidx.compose.material3.Icon(Icons.Default.Business, contentDescription = null)
                        Text(stringResource(R.string.branding_pro_only), modifier = Modifier.weight(1f))
                        androidx.compose.material3.Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
        item { SectionTitle(R.string.settings_data_privacy) }
        item { ActionRow(R.string.export_json) { if (state.billing.isPro) exportLauncher.launch("used-phone-inspections.json") else onUpgrade() } }
        item { ActionRow(R.string.import_json) { if (state.billing.isPro) importLauncher.launch(arrayOf("application/json")) else onUpgrade() } }
        item { ActionRow(R.string.delete_temporary_files, viewModel::deleteTemporaryFiles) }
        item { ActionRow(R.string.delete_all_data) { deleteAll = true } }
        item { ActionRow(R.string.privacy_policy) { privacy = true } }
        item {
            ActionRow(R.string.application_settings) {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }
        }
        item { SectionTitle(R.string.settings_purchase) }
        item { Text(stringResource(R.string.current_plan) + ": " + stringResource(if (state.billing.isPro) R.string.pro_plan else R.string.free_plan)) }
        if (!state.billing.isPro) item { ActionRow(R.string.buy_lifetime_pro, onUpgrade) }
        item { ActionRow(R.string.restore_purchase, viewModel.billingRepository::restorePurchases) }
        item { Text(stringResource(R.string.billing_diagnostics) + ": " + state.billing.connection.name) }
    }
    if (deleteAll) {
        AlertDialog(
            onDismissRequest = { deleteAll = false },
            title = { Text(stringResource(R.string.delete_all_title)) },
            text = { Text(stringResource(R.string.delete_all_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAllData(); deleteAll = false }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteAll = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (privacy) {
        AlertDialog(
            onDismissRequest = { privacy = false },
            title = { Text(stringResource(R.string.privacy_policy)) },
            text = { Text(stringResource(R.string.privacy_summary)) },
            confirmButton = { TextButton(onClick = { privacy = false }) { Text(stringResource(R.string.done)) } },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.brandingFields(
    value: BrandingProfileEntity,
    onChange: (BrandingProfileEntity) -> Unit,
) {
    item { BrandingField(R.string.business_name, value.businessName) { onChange(value.copy(businessName = it)) } }
    item { BrandingField(R.string.address, value.address) { onChange(value.copy(address = it)) } }
    item { BrandingField(R.string.phone_number, value.phone) { onChange(value.copy(phone = it)) } }
    item { BrandingField(R.string.email, value.email) { onChange(value.copy(email = it)) } }
    item { BrandingField(R.string.report_title, value.reportTitle) { onChange(value.copy(reportTitle = it)) } }
    item { BrandingField(R.string.report_footer, value.footerText) { onChange(value.copy(footerText = it)) } }
}

@Composable
private fun BrandingField(label: Int, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(500)) },
        label = { Text(stringResource(label)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionTitle(label: Int) { Text(stringResource(label), style = MaterialTheme.typography.titleLarge) }

@Composable
private fun SwitchRow(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        Switch(checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(label: Int, action: () -> Unit) {
    TextButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        androidx.compose.material3.Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun <T> ChoiceRow(
    label: Int,
    entries: List<T>,
    current: T,
    text: @Composable (T) -> String,
    onChange: (T) -> Unit,
) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.forEach { entry ->
                TextButton(onClick = { onChange(entry) }, modifier = Modifier.weight(1f)) {
                    Text((if (entry == current) "✓ " else "") + text(entry))
                }
            }
        }
    }
}
