package com.magisk.next.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.magisk.next.AppSettings
import com.magisk.next.Logger
import com.magisk.next.MainActivity
import com.magisk.next.R
import com.magisk.next.root.RootManager
import com.magisk.next.root.RootStatus
import androidx.compose.foundation.background
import com.magisk.next.ui.theme.*
import kotlinx.coroutines.launch
import com.magisk.next.ui.theme.BrandCyan
import com.magisk.next.ui.settings.RootSettings
import com.magisk.next.ui.settings.AboutSettings
import com.magisk.next.ui.settings.LoggingSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf<String?>(null) }

    if (selectedCategory != null) {
        BackHandler { selectedCategory = null }
    }

    if (selectedCategory == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title), color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                CategoryCard(
                    title = stringResource(R.string.settings_category_general),
                    icon = Icons.Default.Settings,
                    onClick = { selectedCategory = "general" }
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategoryCard(
                    title = stringResource(R.string.settings_category_theme),
                    icon = AppIcons.Palette,
                    onClick = { selectedCategory = "theme" }
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategoryCard(
                    title = stringResource(R.string.settings_category_saving),
                    icon = AppIcons.Save,
                    onClick = { selectedCategory = "saving" }
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategoryCard(
                    title = stringResource(R.string.settings_category_browser),
                    icon = AppIcons.Folder,
                    onClick = { selectedCategory = "browser" }
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategoryCard(
                    title = stringResource(R.string.settings_category_logging),
                    icon = AppIcons.ListAlt,
                    onClick = { selectedCategory = "logging" }
                )
                Spacer(modifier = Modifier.height(12.dp))
                // [+] Root-доступ
                CategoryCard(
                    title = "Root",
                    icon = Icons.Default.Lock,
                    onClick = { selectedCategory = "root" }
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategoryCard(
                    title = stringResource(R.string.settings_category_about),
                    icon = Icons.Default.Info,
                    onClick = { selectedCategory = "about" }
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (selectedCategory) {
                                "general" -> stringResource(R.string.settings_category_general)
                                "theme" -> stringResource(R.string.settings_category_theme)
                                "saving" -> stringResource(R.string.settings_category_saving)
                                "browser" -> stringResource(R.string.settings_category_browser)
                                "logging" -> stringResource(R.string.settings_category_logging)
                                "root" -> "Root"
                                "about" -> stringResource(R.string.settings_category_about)
                                else -> ""
                            },
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedCategory = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedCategory) {
                    "general" -> GeneralSettings(settings, context)
                    "theme" -> ThemeSettings(settings, context)
                    "saving" -> SavingSettings(settings, context, scope)
                    "browser" -> BrowserSettings(settings, context)
                    "logging" -> LoggingSettings(settings, context, scope)
                    "root" -> RootSettings(settings, context, scope)
                    "about" -> AboutSettings(settings, context, scope)
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = BrandCyan, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(AppIcons.ChevronRight, null, tint = TextSecondary)
        }
    }
}

// ---------- Общие ----------
@Composable
private fun GeneralSettings(settings: AppSettings, context: android.content.Context) {
    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val currentLang = LocaleHelper.getSavedLanguage(context)
        Button(
            onClick = {
                if (currentLang != "en") {
                    LocaleHelper.setLocale(context, "en")
                    context.startActivity(Intent(context, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) })
                    (context as? Activity)?.finish()
                }
            },
            modifier = if (currentLang == "en") Modifier.background(BrandGradient, ButtonDefaults.shape) else Modifier,
            colors = ButtonDefaults.buttonColors(containerColor = if (currentLang == "en") Color.Transparent else BgTertiary)
        ) { Text("English", color = if (currentLang == "en") Color.White else TextPrimary) }
        Button(
            onClick = {
                if (currentLang != "ru") {
                    LocaleHelper.setLocale(context, "ru")
                    context.startActivity(Intent(context, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) })
                    (context as? Activity)?.finish()
                }
            },
            modifier = if (currentLang == "ru") Modifier.background(BrandGradient, ButtonDefaults.shape) else Modifier,
            colors = ButtonDefaults.buttonColors(containerColor = if (currentLang == "ru") Color.Transparent else BgTertiary)
        ) { Text("Русский", color = if (currentLang == "ru") Color.White else TextPrimary) }
    }
}

// ---------- Оформление ----------
@Composable
private fun ThemeSettings(settings: AppSettings, context: android.content.Context) {
    val themeOptions = listOf(
        "system" to stringResource(R.string.theme_system),
        "light" to stringResource(R.string.theme_light),
        "dark" to stringResource(R.string.theme_dark),
        "amoled" to stringResource(R.string.theme_amoled)
    )
    var currentTheme by remember { mutableStateOf(settings.themeMode) }

    Text(stringResource(R.string.settings_category_theme), style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
    themeOptions.forEach { (value, label) ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable {
                currentTheme = value
                settings.themeMode = value
                context.startActivity(Intent(context, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) })
                (context as? Activity)?.finish()
            }.padding(vertical = 8.dp)
        ) {
            RadioButton(
                selected = currentTheme == value,
                onClick = {
                    currentTheme = value
                    settings.themeMode = value
                    context.startActivity(Intent(context, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) })
                    (context as? Activity)?.finish()
                },
                colors = RadioButtonDefaults.colors(selectedColor = Accent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = TextPrimary)
        }
    }
}

// ---------- Сохранение ----------
@Composable
private fun SavingSettings(settings: AppSettings, context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope) {
    var savePath by remember { mutableStateOf(settings.savePath) }
    var autoSave by remember { mutableStateOf(settings.autoSaveOnExit) }
    var notifyExport by remember { mutableStateOf(settings.notifyAfterExport) }

    var showPathDialog by remember { mutableStateOf(false) }
    var newPath by remember { mutableStateOf(settings.savePath) }
    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
                val parts = docId.split(":")
                newPath = if (parts.size == 2 && parts[0] == "primary") parts[1] else docId
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.toast_path_read_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Text(stringResource(R.string.settings_save_path), style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
    Text(savePath, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = {
            settings.savePath = "MagiskModuleCreator/Project"
            savePath = settings.savePath
        }) { Text(stringResource(R.string.settings_reset), color = TextSecondary) }
        TextButton(onClick = { showPathDialog = true }) { Text(buildAnnotatedString { withStyle(SpanStyle(brush = BrandGradient)) { append(stringResource(R.string.settings_change)) } }, fontWeight = FontWeight.SemiBold) }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(stringResource(R.string.settings_auto_save_exit), style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
    Switch(
        checked = autoSave,
        onCheckedChange = {
            autoSave = it
            settings.autoSaveOnExit = it
        },
        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandCyan)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(stringResource(R.string.settings_notify_export), style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
    Switch(
        checked = notifyExport,
        onCheckedChange = {
            notifyExport = it
            settings.notifyAfterExport = it
        },
        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandCyan)
    )

    if (showPathDialog) {
        AlertDialog(
            onDismissRequest = { showPathDialog = false },
            title = { Text(stringResource(R.string.settings_path_dialog_title), color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(value = newPath, onValueChange = { newPath = it }, label = { Text(stringResource(R.string.settings_path_label)) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors())
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { folderPickerLauncher.launch(null) }) { Text(stringResource(R.string.settings_browse_folder), color = Accent) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.savePath = newPath.ifBlank { "MagiskModuleCreator/Project" }
                    savePath = settings.savePath
                    showPathDialog = false
                }) { Text(stringResource(R.string.settings_save), color = Accent) }
            },
            dismissButton = { TextButton(onClick = { showPathDialog = false }) { Text(stringResource(R.string.settings_cancel), color = TextSecondary) } }
        )
    }
}

// ---------- Файловый браузер ----------
@Composable
private fun BrowserSettings(settings: AppSettings, context: android.content.Context) {
    var showHidden by remember { mutableStateOf(settings.showHiddenFiles) }

    Text(stringResource(R.string.settings_show_hidden_files), style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
    Switch(
        checked = showHidden,
        onCheckedChange = {
            showHidden = it
            settings.showHiddenFiles = it
        },
        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandCyan)
    )
}

// ---------- Логирование ----------