package com.magisk.next.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.magisk.next.AppSettings
import com.magisk.next.Logger
import com.magisk.next.R
import com.magisk.next.ui.theme.*
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment

@Composable
fun LoggingSettings(settings: AppSettings, context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope) {
    var logLevel by remember { mutableStateOf(settings.logLevel) }
    var showLogLevelDialog by remember { mutableStateOf(false) }

    Text(stringResource(R.string.settings_log_level), style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
    val levelText = when (logLevel) {
        "errors" -> stringResource(R.string.settings_log_level_errors)
        "errors+warnings" -> stringResource(R.string.settings_log_level_warnings)
        "all" -> stringResource(R.string.settings_log_level_all)
        else -> logLevel
    }
    Text(levelText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
    TextButton(onClick = { showLogLevelDialog = true }) { Text(buildAnnotatedString { withStyle(SpanStyle(brush = BrandGradient)) { append(stringResource(R.string.settings_change)) } }, fontWeight = FontWeight.SemiBold) }

    Spacer(modifier = Modifier.height(24.dp))

    Text(stringResource(R.string.settings_logs), style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
    Button(onClick = {
        // [*] suspend-функция — вызов через корутину, I/O убран с main thread
        scope.launch {
            settings.clearLogs()
            Toast.makeText(context, context.getString(R.string.settings_logs_cleared), Toast.LENGTH_SHORT).show()
        }
    }) { Text(buildAnnotatedString { withStyle(SpanStyle(brush = BrandGradient)) { append(stringResource(R.string.settings_clear_logs)) } }, fontWeight = FontWeight.SemiBold) }

    if (showLogLevelDialog) {
        val levels = listOf("errors", "errors+warnings", "all")
        val names = listOf(
            stringResource(R.string.settings_log_level_errors),
            stringResource(R.string.settings_log_level_warnings),
            stringResource(R.string.settings_log_level_all)
        )
        AlertDialog(
            onDismissRequest = { showLogLevelDialog = false },
            title = { Text(stringResource(R.string.settings_log_level), color = TextPrimary) },
            text = {
                Column {
                    levels.forEachIndexed { index, level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable {
                                logLevel = level
                                settings.logLevel = level
                                Logger.setLevel(level)  // [+] применяем уровень сразу, без перезапуска
                                showLogLevelDialog = false
                            }
                        ) {
                            RadioButton(selected = logLevel == level, onClick = {
                                logLevel = level
                                settings.logLevel = level
                                Logger.setLevel(level)  // [+] применяем уровень сразу, без перезапуска
                                showLogLevelDialog = false
                            }, colors = RadioButtonDefaults.colors(selectedColor = Accent))
                            Text(names[index], modifier = Modifier.padding(start = 4.dp), color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLogLevelDialog = false }) { Text(stringResource(R.string.settings_cancel), color = Accent) } }
        )
    }
}

// ---------- Root ----------