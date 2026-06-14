package com.magisk.next.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
import com.magisk.next.R
import com.magisk.next.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AboutSettings(settings: AppSettings, context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope) {
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(settings.exportSettings().toByteArray()) }
                    Toast.makeText(context, context.getString(R.string.settings_export_success), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) { Toast.makeText(context, context.getString(R.string.settings_export_error), Toast.LENGTH_SHORT).show() }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonStr = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                    settings.importSettings(jsonStr)
                    Toast.makeText(context, context.getString(R.string.settings_import_success), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) { Toast.makeText(context, context.getString(R.string.settings_import_error), Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // [+] Версия приложения из PackageManager
    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—" }
        catch (_: Exception) { "—" }
    }

    // [+] Фирменная шапка: логотип N + название + автор
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = TextPrimary)) { append("NextMod ") }
                withStyle(SpanStyle(brush = BrandGradient)) { append("IDE") }
            },
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            stringResource(R.string.about_author),
            style = MaterialTheme.typography.bodySmall,
            color = BrandCyan,
            fontWeight = FontWeight.Medium
        )
    }
    Spacer(modifier = Modifier.height(20.dp))

    Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
    Text(stringResource(R.string.settings_about_content), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        stringResource(R.string.settings_version_label, versionName),
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted
    )

    Spacer(modifier = Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { exportLauncher.launch("magisk_module_creator_settings.json") }) { Text(buildAnnotatedString { withStyle(SpanStyle(brush = BrandGradient)) { append(stringResource(R.string.settings_export)) } }, fontWeight = FontWeight.SemiBold) }
        TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text(buildAnnotatedString { withStyle(SpanStyle(brush = BrandGradient)) { append(stringResource(R.string.settings_import)) } }, fontWeight = FontWeight.SemiBold) }
    }
}