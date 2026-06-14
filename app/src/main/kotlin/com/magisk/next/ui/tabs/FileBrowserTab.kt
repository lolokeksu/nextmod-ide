package com.magisk.next.ui.tabs

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import com.magisk.next.ui.theme.AppIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.magisk.next.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magisk.next.AppSettings
import com.magisk.next.viewmodel.ModuleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.magisk.next.ui.theme.Accent
import com.magisk.next.ui.theme.BgPrimary
import com.magisk.next.ui.theme.BrandCyan

private val allHiddenExtensions = listOf(
    "zip", "7z", "rar", "tar", "gz", "bz2", "xz", "lz", "lz4", "lzma", "zst",
    "cab", "arj", "ace", "tgz", "tbz2", "txz", "tlz", "tlz4",
    "apk", "jar", "war", "ear", "sar",
    "tmp", "temp", "log", "bak", "db", "db-journal",
    "dex", "odex", "vdex",
    "png", "jpg", "jpeg", "gif", "webp", "bmp",
    "mp3", "ogg", "wav", "flac", "aac",
    "mp4", "mkv", "avi", "mov", "webm",
    "ttf", "otf",
    "txt", "md", "doc", "docx", "pdf", "xls", "xlsx", "ppt", "pptx",
    "pages", "numbers", "key",
    "csv", "json", "xml", "html", "htm", "css", "js",
    "iso", "img", "dmg", "vmdk", "vhd", "vhdx",
    "bin", "exe", "java", "env", "kt",
    "torrent", "magnet"
)

@Composable
fun FileBrowserTab(viewModel: ModuleViewModel) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context.applicationContext) }
    val rootDir = Environment.getExternalStorageDirectory().absolutePath
    var currentDir by remember { mutableStateOf(rootDir) }
    var sortAscending by remember { mutableStateOf(true) }
    var sortByDate by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // [+] Файл, ожидающий подтверждения удаления
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    val hiddenExtensionsState = remember {
        val saved = settings.hiddenExtensions
        mutableStateMapOf<String, Boolean>().apply { saved.forEach { this[it] = true } }
    }

    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }

    var files by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(currentDir, hasPermission, sortAscending, sortByDate) {
        if (hasPermission) {
            withContext(Dispatchers.IO) {
                val list = File(currentDir).listFiles()?.filter { f ->
                    if (!settings.showHiddenFiles && f.name.startsWith(".")) return@filter false
                    val ext = f.extension.lowercase()
                    if (hiddenExtensionsState[ext] == true) return@filter false
                    true
                }?.sortedWith { a, b ->
                    if (a.isDirectory != b.isDirectory) {
                        if (a.isDirectory) -1 else 1
                    } else {
                        val cmp = if (sortByDate) {
                            a.lastModified().compareTo(b.lastModified())
                        } else {
                            a.name.lowercase().compareTo(b.name.lowercase())
                        }
                        if (sortAscending) cmp else -cmp
                    }
                }
                files = list ?: emptyList()
            }
        } else {
            files = emptyList()
        }
    }

    val cs = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (currentDir != rootDir) {
                    IconButton(onClick = {
                        val parent = File(currentDir).parentFile
                        if (parent != null && parent.exists() && parent.absolutePath.startsWith(rootDir)) {
                            currentDir = parent.absolutePath
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = cs.onSurfaceVariant)
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                Text(
                    text = currentDir,
                    color = cs.onSurface,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(AppIcons.FilterList, contentDescription = stringResource(R.string.menu_settings), tint = Accent)
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.fb_sort_section), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = cs.onSurfaceVariant) },
                            onClick = { }, enabled = false
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = !sortByDate, onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = Accent))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.fb_sort_by_name), fontSize = 14.sp, color = if (!sortByDate) Accent else cs.onSurface)
                                }
                            },
                            onClick = { sortByDate = false; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = sortByDate, onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = Accent))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.fb_sort_by_date), fontSize = 14.sp, color = if (sortByDate) Accent else cs.onSurface)
                                }
                            },
                            onClick = { sortByDate = true; showSortMenu = false }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.fb_direction_section), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = cs.onSurfaceVariant) },
                            onClick = { }, enabled = false
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = sortAscending, onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = Accent))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.fb_sort_asc), fontSize = 14.sp, color = if (sortAscending) Accent else cs.onSurface)
                                }
                            },
                            onClick = { sortAscending = true; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = !sortAscending, onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = Accent))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.fb_sort_desc), fontSize = 14.sp, color = if (!sortAscending) Accent else cs.onSurface)
                                }
                            },
                            onClick = { sortAscending = false; showSortMenu = false }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.fb_hidden_types), fontSize = 14.sp) },
                            onClick = { showSortMenu = false; showFilterDialog = true },
                            leadingIcon = { Icon(AppIcons.Filter, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }

            HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.2f))

            if (!hasPermission) {
                Box(modifier = Modifier.weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.fb_no_access), color = cs.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }) { Text(text = stringResource(R.string.menu_settings)) }
                    }
                }
            } else if (files.isEmpty()) {
                Box(modifier = Modifier.weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.fb_empty_folder), color = cs.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    // [*] key по absolutePath — корректная анимация при удалении
                    items(files, key = { it.absolutePath }) { file ->
                        if (!file.isDirectory) {
                            // [+] SwipeToDismissBox только для файлов (не папок)
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        fileToDelete = file
                                    }
                                    // false — визуально снапбэк; диалог обработает удаление
                                    false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true,
                                backgroundContent = {
                                    // [+] Красный фон с иконкой удаления при свайпе влево
                                    val bgColor by animateColorAsState(
                                        targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                            cs.errorContainer
                                        else
                                            Color.Transparent,
                                        label = "swipe_bg"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(bgColor)
                                            .padding(end = 16.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.btn_delete),
                                            tint = cs.onErrorContainer
                                        )
                                    }
                                }
                            ) {
                                FileRow(
                                    file = file,
                                    cs = cs,
                                    onAddClick = {
                                        viewModel.addFileFromFile(context, file) {
                                            Toast.makeText(
                                                context,
                                                "✓ ${file.name}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }  // закрывает SwipeToDismissBox content
                        } else {
                            // Папка — свайп не нужен, только навигация
                            FileRow(
                                file = file,
                                cs = cs,
                                onDirClick = { currentDir = file.absolutePath }
                            )
                        }
                    }
                }
            }
        }
    }

    // [+] Диалог подтверждения удаления файла с диска
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = cs.error) },
            title = { Text(stringResource(R.string.dlg_delete_file_title)) },
            text = {
                Text(
                    text = file.name,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFileFromDisk(file)
                        files = files.filter { it.absolutePath != file.absolutePath }
                        fileToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.dlg_delete_confirm), color = cs.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text(stringResource(R.string.dlg_delete_cancel))
                }
            }
        )
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false; settings.hiddenExtensions = hiddenExtensionsState.keys.toSet() },
            title = { Text(stringResource(R.string.fb_hidden_types_title)) },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { allHiddenExtensions.forEach { hiddenExtensionsState[it] = true } }) { Text(stringResource(R.string.fb_select_all)) }
                        TextButton(onClick = { hiddenExtensionsState.clear() }) { Text(stringResource(R.string.fb_deselect_all)) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        allHiddenExtensions.forEach { ext ->
                            val isChecked = hiddenExtensionsState[ext] ?: false
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        hiddenExtensionsState[ext] = checked
                                        if (!checked) hiddenExtensionsState.remove(ext)
                                    }
                                )
                                Text(".$ext", fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.hiddenExtensions = hiddenExtensionsState.keys.toSet()
                    showFilterDialog = false
                }) { Text(stringResource(R.string.fb_close)) }
            }
        )
    }
}

// [+] Вынесенный Composable для строки файла/папки — устраняет дублирование кода
@Composable
private fun FileRow(
    file: File,
    cs: ColorScheme,
    onAddClick: (() -> Unit)? = null,
    onDirClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgPrimary)
            .clickable(enabled = file.isDirectory) { onDirClick?.invoke() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ic = if (file.isDirectory) AppIcons.Folder else AppIcons.InsertDriveFile
        val tint = if (file.isDirectory) BrandCyan else cs.onSurfaceVariant
        Icon(ic, null, tint = tint)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = file.name, color = cs.onSurface, fontSize = 14.sp)
            if (!file.isDirectory) {
                val kb = file.length() / 1024
                Text(text = "${kb} KB", color = cs.onSurface.copy(alpha = 0.6f), fontSize = 10.sp)
            }
        }
        if (!file.isDirectory && onAddClick != null) {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, null, tint = Accent)
            }
        }
    }
}