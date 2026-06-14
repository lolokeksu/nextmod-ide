package com.magisk.next.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.magisk.next.AppSettings
import com.magisk.next.R
import com.magisk.next.root.RootManager
import com.magisk.next.root.RootStatus
import com.magisk.next.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RootSettings(
    settings: AppSettings,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var rootEnabled by remember { mutableStateOf(settings.rootEnabled) }
    var rootStatus  by remember { mutableStateOf(RootStatus.UNAVAILABLE) }
    var managerInfo by remember { mutableStateOf("") }
    var isChecking  by remember { mutableStateOf(false) }

    // Проверяем статус при открытии экрана и при включении root
    LaunchedEffect(rootEnabled) {
        if (rootEnabled) {
            isChecking = true
            rootStatus  = RootManager.getStatus()
            managerInfo = if (rootStatus == RootStatus.GRANTED) RootManager.getManagerInfo() else ""
            isChecking  = false
        } else {
            rootStatus  = RootStatus.UNAVAILABLE
            managerInfo = ""
        }
    }

    // ── Переключатель ────────────────────────────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_root_enable),
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                stringResource(R.string.settings_root_enable_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = rootEnabled,
            onCheckedChange = { enabled ->
                rootEnabled = enabled
                settings.rootEnabled = enabled
                if (enabled) {
                    RootManager.init(debug = false)
                    scope.launch {
                        isChecking = true
                        rootStatus = RootManager.requestRoot()
                        managerInfo = if (rootStatus == RootStatus.GRANTED) RootManager.getManagerInfo() else ""
                        isChecking = false
                    }
                }
            },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandCyan)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ── Статус ───────────────────────────────────────────────────────────────
    if (rootEnabled) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_root_checking), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        val (statusText, statusColor) = when (rootStatus) {
                            RootStatus.GRANTED     -> stringResource(R.string.settings_root_status_granted)     to Color(0xFF10B981)
                            RootStatus.DENIED      -> stringResource(R.string.settings_root_status_denied)      to Color(0xFFEF4444)
                            RootStatus.UNAVAILABLE -> stringResource(R.string.settings_root_status_unavailable) to TextSecondary
                        }
                        Icon(
                            Icons.Default.Lock,
                            null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_root_status_label, statusText),
                            color = statusColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (managerInfo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_root_manager_label, managerInfo),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (rootStatus != RootStatus.GRANTED && !isChecking) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            scope.launch {
                                isChecking = true
                                rootStatus = RootManager.requestRoot()
                                managerInfo = if (rootStatus == RootStatus.GRANTED) RootManager.getManagerInfo() else ""
                                isChecking = false
                            }
                        }
                    ) { Text(buildAnnotatedString { withStyle(SpanStyle(brush = BrandGradient)) { append(stringResource(R.string.settings_root_request)) } }, fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Что даёт root ────────────────────────────────────────────────────
        Text(
            stringResource(R.string.settings_root_features_title),
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        val features = listOf(
            stringResource(R.string.root_feature_direct_install),
            stringResource(R.string.root_feature_manager),
            stringResource(R.string.root_feature_toggle),
            stringResource(R.string.root_feature_browser),
            stringResource(R.string.root_feature_conflicts)
        )
        features.forEach { feature ->
            Text(
                feature,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Предупреждение ───────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1F00)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.settings_root_warning),
                    color = Color(0xFFF59E0B),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}