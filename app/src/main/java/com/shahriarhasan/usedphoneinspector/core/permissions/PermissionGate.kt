package com.shahriarhasan.usedphoneinspector.core.permissions

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.design.InformationCard

@Composable
fun PermissionGate(
    feature: PermissionFeature,
    title: Int,
    rationale: Int,
    grantLabel: Int,
    onDenied: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val permissions = remember(feature) { PermissionPolicy.permissionsFor(feature) }
    fun granted(): Boolean = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    var isGranted by remember(permissions.contentHashCode()) { mutableStateOf(permissions.isEmpty() || granted()) }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        isGranted = result.values.all { it }
        denied = !isGranted
        if (denied) onDenied()
    }
    if (isGranted) {
        content()
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InformationCard(stringResource(title), stringResource(rationale))
            Button(onClick = { launcher.launch(permissions) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(grantLabel))
            }
            if (denied) {
                Text(stringResource(R.string.permission_denied_body), color = MaterialTheme.colorScheme.error)
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.open_settings)) }
            }
        }
    }
}

