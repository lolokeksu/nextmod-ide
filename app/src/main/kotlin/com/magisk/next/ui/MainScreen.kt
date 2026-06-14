package com.magisk.next.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magisk.next.AppSettings
import com.magisk.next.Logger
import com.magisk.next.MainActivity
import com.magisk.next.R
import com.magisk.next.root.RootManager
import com.magisk.next.util.installModule
import com.magisk.next.util.installModuleWithRoot
import com.magisk.next.util.getZipFilePath
import com.magisk.next.util.formatBytes
import com.magisk.next.util.resolveProjectPath
import com.magisk.next.util.openRecentProject
import com.magisk.next.util.formatRelativeTime
import com.magisk.next.root.RootStatus
import com.magisk.next.ui.tabs.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.magisk.next.ui.theme.Accent
import com.magisk.next.ui.theme.BgPrimary
import com.magisk.next.ui.theme.BgTertiary
import com.magisk.next.ui.theme.BrandCyan
import com.magisk.next.ui.theme.TextMuted
import com.magisk.next.ui.theme.BrandGradient
import com.magisk.next.ui.theme.TextPrimary
import com.magisk.next.ui.theme.TextSecondary
import com.magisk.next.ui.theme.AppIcons
import com.magisk.next.viewmodel.ModuleTemplate
import com.magisk.next.viewmodel.ModuleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tab(val titleResId: Int, val icon: @Composable () -> Unit) {
    METADATA(R.string.tab_metadata, { Icon(Icons.Default.Info, contentDescription = null) }),
    FILES(R.string.tab_files, { Icon(AppIcons.Folder, contentDescription = null) }),
    PREVIEW(R.string.tab_preview, { Icon(AppIcons.Visibility, contentDescription = null) }),
    ADVANCED(R.string.tab_advanced, { Icon(Icons.Default.Settings, contentDescription = null) }),
    BROWSER(R.string.tab_browser, { Icon(Icons.Default.Search, contentDescription = null) })
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: ModuleViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(Tab.METADATA) }
    val context = LocalContext.current
    val activity = context as? Activity
    var showMenu by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    // [+] URI последнего собранного ZIP — для кнопки установки
    var lastExportedUri by remember { mutableStateOf<Uri?>(null) }
    var showInstallDialog by remember { mutableStateOf(false) }
    // [+] Экран менеджера модулей
    var showModuleManager by remember { mutableStateOf(false) }
    // [+] Диалог валидации + превью перед экспортом
    var showPreExportDialog by remember { mutableStateOf(false) }
    // [+] Подтверждение сброса
    var showResetDialog by remember { mutableStateOf(false) }

    // [+] Root состояние
    val appSettings = remember { AppSettings(context) }
    var rootStatus by remember { mutableStateOf(RootStatus.UNAVAILABLE) }
    // [+] Недавние проекты — объявлен ПОСЛЕ appSettings
    var recentProjects by remember { mutableStateOf(appSettings.getRecentProjects()) }

    // Проверяем статус root при старте и когда rootEnabled меняется
    LaunchedEffect(appSettings.rootEnabled) {
        if (appSettings.rootEnabled) {
            rootStatus = withContext(Dispatchers.IO) { RootManager.getStatus() }
        } else {
            rootStatus = RootStatus.UNAVAILABLE
        }
    }

    val scope = rememberCoroutineScope()

    // Лаунчер сохранения проекта
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.saveProject(context, uri) { success ->
                if (success) {
                    Logger.logInfo("Проект сохранён: $uri")
                    Toast.makeText(context, context.getString(R.string.toast_project_saved), Toast.LENGTH_SHORT).show()
                    // [+] Добавляем в недавние
                    resolveProjectPath(context, uri)?.let { (path, name) ->
                        appSettings.addRecentProject(path, name)
                        recentProjects = appSettings.getRecentProjects()
                    }
                } else {
                    Logger.logError("Ошибка сохранения проекта")
                    Toast.makeText(context, context.getString(R.string.toast_save_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val loadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadProject(context, uri) { success ->
                if (success) {
                    Logger.logInfo("Проект загружен: $uri")
                    Toast.makeText(context, context.getString(R.string.toast_project_loaded), Toast.LENGTH_SHORT).show()
                    // [+] Добавляем в недавние
                    resolveProjectPath(context, uri)?.let { (path, name) ->
                        appSettings.addRecentProject(path, name)
                        recentProjects = appSettings.getRecentProjects()
                    }
                } else {
                    Logger.logError("Ошибка загрузки проекта")
                    Toast.makeText(context, context.getString(R.string.toast_load_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // [+] Отдельный лаунчер импорта готового Magisk-модуля (.zip)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importModule(context, uri)
            Toast.makeText(context, context.getString(R.string.toast_imported), Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = true) {
        when {
            showModuleManager -> showModuleManager = false
            showSettings -> showSettings = false
            else -> showExitDialog = true
        }
    }

    if (showModuleManager) {
        ModuleManagerScreen(onBack = { showModuleManager = false })
        return
    }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    val pagerState = rememberPagerState(pageCount = { Tab.entries.size })

    LaunchedEffect(selectedTab) {
        pagerState.animateScrollToPage(selectedTab.ordinal)
    }
    LaunchedEffect(pagerState.currentPage) {
        selectedTab = Tab.entries[pagerState.currentPage]
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // [*] Фирменный логотип-надпись, зеркально сплешу:
                    //     бренд не локализуется, поэтому литералы
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = TextPrimary)) { append("NextMod\n") }
                            withStyle(SpanStyle(brush = BrandGradient)) { append("IDE") }
                        },
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 17.sp
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Accent
                ),
                actions = {
                    // [+] Индикатор root-статуса — только если root включён в настройках
                    if (appSettings.rootEnabled) {
                        val (rootIcon, rootTint) = when (rootStatus) {
                            RootStatus.GRANTED     -> Icons.Default.Lock to Color(0xFF10B981)
                            RootStatus.DENIED      -> Icons.Default.Lock to Color(0xFFEF4444)
                            RootStatus.UNAVAILABLE -> Icons.Default.Lock to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        IconButton(onClick = {
                            scope.launch {
                                rootStatus = withContext(Dispatchers.IO) { RootManager.requestRoot() }
                            }
                        }) {
                            Icon(rootIcon, contentDescription = stringResource(R.string.cd_root_status), tint = rootTint)
                        }
                    }
                    TextButton(onClick = { viewModel.resetToTemplate() }) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(brush = BrandGradient)) {
                append(stringResource(R.string.action_template))
            }
        },
        fontWeight = FontWeight.SemiBold
    )
}
TextButton(onClick = { showResetDialog = true }) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(brush = BrandGradient)) {
                append(stringResource(R.string.action_reset))
            }
        },
        fontWeight = FontWeight.SemiBold
    )
}

                    // Кнопка «Экспорт»
                    TextButton(
    onClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.toast_permission_manual), Toast.LENGTH_LONG).show()
            }
            return@TextButton
        }
        showPreExportDialog = true
    },
    enabled = !isExporting
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(brush = if (!isExporting) BrandGradient else SolidColor(BgTertiary))) {
                append(stringResource(R.string.action_export))
            }
        },
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    )
}

                    // Меню
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more), tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_save_project)) },
                                onClick = {
                                    showMenu = false
                                    try { saveLauncher.launch("project.mmproj") }
                                    catch (e: Exception) { Toast.makeText(context, context.getString(R.string.toast_file_manager_unavailable), Toast.LENGTH_SHORT).show() }
                                },
                                leadingIcon = { Icon(AppIcons.Save, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_open_project)) },
                                onClick = {
                                    showMenu = false
                                    try { loadLauncher.launch(arrayOf("*/*")) }
                                    catch (e: Exception) { Toast.makeText(context, context.getString(R.string.toast_file_manager_unavailable), Toast.LENGTH_SHORT).show() }
                                },
                                leadingIcon = { Icon(AppIcons.FolderOpen, contentDescription = null) }
                            )
                            if (recentProjects.isNotEmpty()) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_recent_projects), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = {}, enabled = false
                                )
                                recentProjects.forEach { project: AppSettings.RecentProject ->
                                    // [*] exists проверяется напрямую без remember — дешёвая операция
                                    val exists = java.io.File(project.path).exists()
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    project.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (exists) MaterialTheme.colorScheme.onSurface
                                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    formatRelativeTime(context, project.timestamp),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            showMenu = false
                                            openRecentProject(context, project, appSettings,
                                                onLoaded = { uri ->
                                                    viewModel.loadProject(context, uri) { success ->
                                                        if (success) {
                                                            appSettings.addRecentProject(project.path, project.name)
                                                            recentProjects = appSettings.getRecentProjects()
                                                            Toast.makeText(context, context.getString(R.string.toast_project_opened), Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                onNotFound = {
                                                    appSettings.removeRecentProject(project.path)
                                                    recentProjects = appSettings.getRecentProjects()
                                                    Toast.makeText(context, context.getString(R.string.toast_file_not_found), Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        },
                                        enabled = exists,
                                        leadingIcon = { Icon(AppIcons.Folder, null, tint = if (exists) BrandCyan else TextMuted) }
                                    )
                                }
                                HorizontalDivider()
                            }
                            // [+] Явный пункт для импорта готового Magisk ZIP
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_import_module)) },
                                onClick = {
                                    showMenu = false
                                    try { importLauncher.launch(arrayOf("application/zip", "*/*")) }
                                    catch (e: Exception) { Toast.makeText(context, context.getString(R.string.toast_file_manager_unavailable), Toast.LENGTH_SHORT).show() }
                                },
                                leadingIcon = { Icon(AppIcons.UploadFile, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_open_folder)) },
                                onClick = {
                                    showMenu = false
                                    try {
                                        val projectDir = java.io.File(Environment.getExternalStorageDirectory(), "MagiskModuleCreator/Project")
                                        if (!projectDir.exists()) projectDir.mkdirs()
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(
                                                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", projectDir),
                                                "resource/folder"
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.toast_folder_open_error), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                leadingIcon = { Icon(AppIcons.FolderOpen, contentDescription = null) }
                            )
                            // [+] Установить последний собранный модуль (только если есть экспорт)
                            lastExportedUri?.let { uri ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_install_module)) },
                                    onClick = {
                                        showMenu = false
                                        installModule(context, uri)
                                    },
                                    leadingIcon = { Icon(AppIcons.UploadFile, contentDescription = null) }
                                )
                            }
                            // [+] Менеджер модулей — только если root включён
                            if (appSettings.rootEnabled) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_module_manager)) },
                                    onClick = {
                                        showMenu = false
                                        showModuleManager = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_settings)) },
                                onClick = {
                                    showMenu = false
                                    showSettings = true
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dlg_template_title)) },
                                onClick = { showMenu = false; showTemplateDialog = true },
                                leadingIcon = { Icon(AppIcons.AutoAwesome, contentDescription = null) }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // [*] Кастомные таблетки-табы в стиле иконки:
            //     активный — градиент BrandGradient + белый текст,
            //     неактивный — тёмный фон + приглушённый текст
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = BgPrimary,
                contentColor = Accent,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    // [*] Скрываем стандартный индикатор — он не нужен при таблетках
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(
                                tabPositions[pagerState.currentPage]
                            ),
                            height = 0.dp,
                            color = Color.Transparent
                        )
                    }
                },
                divider = {}
            ) {
                Tab.entries.forEachIndexed { index, tab ->
                    val selected = pagerState.currentPage == index
                    val tabShape = RoundedCornerShape(50)
                    Tab(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        selectedContentColor = Color.White,
                        unselectedContentColor = TextSecondary,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .background(
                                brush = if (selected) BrandGradient
                                        else SolidColor(BgTertiary),
                                shape = tabShape
                            )
                    ) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            // Иконка таба
                            androidx.compose.runtime.CompositionLocalProvider(
                                androidx.compose.material3.LocalContentColor provides
                                    if (selected) Color.White else TextSecondary
                            ) {
                                Box(modifier = Modifier.size(16.dp)) { tab.icon() }
                            }
                            Text(
                                stringResource(tab.titleResId),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold
                                             else FontWeight.Normal,
                                color = if (selected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (page) {
                        Tab.METADATA.ordinal -> MetadataTab(viewModel)
                        Tab.FILES.ordinal -> FilesTab(viewModel)
                        Tab.PREVIEW.ordinal -> PreviewTab(viewModel)
                        Tab.ADVANCED.ordinal -> AdvancedTab(viewModel)
                        Tab.BROWSER.ordinal -> FileBrowserTab(viewModel)
                    }
                }
            }
        }
    }

    // Прогресс экспорта (по желанию можно оставить, но теперь isExporting меняется в колбэке)
    if (isExporting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.export_progress_title)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.export_progress_message))
                }
            },
            confirmButton = {}
        )
    }

    // Диалог шаблонов
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text(stringResource(R.string.dlg_template_title)) },
            text = {
                Column {
                    Text("• ${stringResource(R.string.dlg_template_empty)}", modifier = Modifier.clickable {
                        viewModel.applyTemplate(ModuleTemplate.EMPTY)
                        showTemplateDialog = false
                    })
                    Text("• ${stringResource(R.string.dlg_template_debloater)}", modifier = Modifier.clickable {
                        viewModel.applyTemplate(ModuleTemplate.DEBLOATER)
                        showTemplateDialog = false
                    })
                    Text("• ${stringResource(R.string.dlg_template_hosts_blocker)}", modifier = Modifier.clickable {
                        viewModel.applyTemplate(ModuleTemplate.HOSTS_BLOCKER)
                        showTemplateDialog = false
                    })
                    Text("• ${stringResource(R.string.dlg_template_kernel_tweaks)}", modifier = Modifier.clickable {
                        viewModel.applyTemplate(ModuleTemplate.KERNEL_TWEAKS)
                        showTemplateDialog = false
                    })
                }
            },
            confirmButton = { TextButton(onClick = { showTemplateDialog = false }) { Text(stringResource(R.string.dlg_template_cancel)) } }
        )
    }

    // [+] Диалог подтверждения сброса
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.dlg_reset_title)) },
            text = { Text(stringResource(R.string.dlg_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAll()
                    showResetDialog = false
                }) { Text(stringResource(R.string.dlg_reset_confirm), color = Color(0xFFEF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.dlg_reset_cancel)) }
            }
        )
    }

    // Диалог выхода
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.dlg_exit_title)) },
            text = { Text(stringResource(R.string.dlg_exit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    try { saveLauncher.launch("project.mmproj") }
                    catch (e: Exception) { Toast.makeText(context, context.getString(R.string.toast_file_manager_unavailable), Toast.LENGTH_SHORT).show() }
                    activity?.finish()
                }) { Text(stringResource(R.string.dlg_exit_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false; activity?.finish() }) { Text(stringResource(R.string.dlg_exit_no)) }
            }
        )
    }

    // [+] Диалог валидации + превью структуры ZIP перед экспортом
    if (showPreExportDialog) {
        val validation = viewModel.validateForExport(context)
        val zipEntries = viewModel.previewZipStructure()
        val totalSize = zipEntries.sumOf { it.size }

        AlertDialog(
            onDismissRequest = { showPreExportDialog = false },
            title = { Text(if (validation.isValid) stringResource(R.string.dlg_export_ready_title) else stringResource(R.string.dlg_export_check_title)) },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    // Ошибки
                    if (validation.errors.isNotEmpty()) {
                        Text(stringResource(R.string.dlg_export_errors), color = Color(0xFFEF4444), style = MaterialTheme.typography.titleSmall)
                        validation.errors.forEach {
                            Text("✕ $it", color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    // Предупреждения
                    if (validation.warnings.isNotEmpty()) {
                        Text(stringResource(R.string.dlg_export_warnings), color = Color(0xFFF59E0B), style = MaterialTheme.typography.titleSmall)
                        validation.warnings.forEach {
                            Text("⚠ $it", color = Color(0xFFF59E0B), style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    // Структура ZIP
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.dlg_export_structure, zipEntries.size, formatBytes(totalSize)),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    zipEntries.forEach { entry ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                entry.path,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (entry.size > 0) formatBytes(entry.size) else "—",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPreExportDialog = false
                        isExporting = true
                        viewModel.buildAndSaveZip(context) { uri ->
                            isExporting = false
                            if (uri != null) {
                                Logger.logInfo("Модуль экспортирован: $uri")
                                lastExportedUri = uri
                                showInstallDialog = true
                            } else {
                                Logger.logError("Ошибка экспорта модуля")
                                Toast.makeText(context, context.getString(R.string.toast_build_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = validation.isValid,
                    modifier = Modifier.background(
                        brush = if (validation.isValid) BrandGradient else SolidColor(BgTertiary),
                        shape = ButtonDefaults.shape
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                ) { Text(
                    if (validation.isValid) stringResource(R.string.dlg_export_build) else stringResource(R.string.dlg_export_fix_errors),
                    color = Color.White, fontWeight = FontWeight.SemiBold
                ) }
            },
            dismissButton = {
                TextButton(onClick = { showPreExportDialog = false }) { Text(stringResource(R.string.dlg_export_cancel)) }
            }
        )
    }

    // [+] Диалог после успешного экспорта с кнопкой установки
    if (showInstallDialog) {
        val uri = lastExportedUri
        val zipPath = getZipFilePath(viewModel.moduleId, viewModel.moduleVersion)
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            icon = { Icon(AppIcons.Save, null, tint = BrandCyan) },
            title = { Text(stringResource(R.string.dlg_install_title)) },
            text = {
                Text(
                    "${viewModel.moduleId.ifBlank { "module" }}-${viewModel.moduleVersion.ifBlank { "1.0.0" }}.zip\n\n" + stringResource(R.string.dlg_install_question),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // [+] Root-установка — прямая, без Intent (только если root доступен)
                    if (appSettings.rootEnabled && rootStatus == RootStatus.GRANTED) {
                        Button(
                            onClick = {
                                showInstallDialog = false
                                installModuleWithRoot(scope, context, zipPath)
                            },
                            modifier = Modifier.background(BrandGradient, ButtonDefaults.shape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) { Text(stringResource(R.string.btn_install_root), color = Color.White, fontWeight = FontWeight.SemiBold) }
                    }
                    // Intent-установка как fallback
                    Button(
                        onClick = {
                            showInstallDialog = false
                            if (uri != null) installModule(context, uri)
                        },
                        modifier = Modifier.background(BrandGradient, ButtonDefaults.shape),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) { Text(stringResource(R.string.btn_install), color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDialog = false }) { Text(stringResource(R.string.btn_install_later)) }
            }
        )
    }
}
// [+] Установка модуля через Intent — пробуем менеджеры по приоритету:
//     Magisk → KernelSU → APatch → универсальный chooser
