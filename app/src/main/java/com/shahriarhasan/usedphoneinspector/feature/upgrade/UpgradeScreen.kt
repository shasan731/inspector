package com.shahriarhasan.usedphoneinspector.feature.upgrade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.billing.BillingConnectionState
import com.shahriarhasan.usedphoneinspector.core.billing.BillingUiState
import com.shahriarhasan.usedphoneinspector.core.billing.PurchaseState

@Composable
fun UpgradeScreen(state: BillingUiState, onBuy: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.upgrade_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.upgrade_body))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    R.string.generate_report,
                    R.string.settings_branding,
                    R.string.export_json,
                    R.string.import_json,
                ).forEach { label ->
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Text(stringResource(label))
                    }
                }
            }
        }
        if (state.isPro) {
            Text(stringResource(R.string.purchase_complete), color = MaterialTheme.colorScheme.primary)
        } else {
            Button(
                onClick = onBuy,
                enabled = state.connection == BillingConnectionState.READY,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.buy_lifetime_pro) + " • " + (state.localizedPrice ?: stringResource(R.string.price_unavailable)))
            }
            OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.restore_purchase))
            }
        }
        when {
            state.connection == BillingConnectionState.CONNECTING -> Text(stringResource(R.string.billing_connecting))
            state.connection == BillingConnectionState.UNAVAILABLE -> Text(stringResource(R.string.billing_unavailable))
            state.purchaseState == PurchaseState.PENDING -> Text(stringResource(R.string.purchase_pending))
            state.purchaseState == PurchaseState.CANCELLED -> Text(stringResource(R.string.purchase_cancelled))
        }
    }
}

