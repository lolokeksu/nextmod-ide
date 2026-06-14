package com.magisk.next.ui.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.magisk.next.ui.theme.ShellSyntaxHighlighter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magisk.next.R
import com.magisk.next.model.ModuleFile
import com.magisk.next.ui.theme.*
import com.magisk.next.viewmodel.ModuleViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.MenuAnchorType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesTab(
    viewModel: ModuleViewModel,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Состояние формы добавления файла
    var fileName by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf("") }
    var filePermissions by remember { mutableStateOf("0644") }
    var fileType by remember { mutableStateOf("text") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var showAddForm by remember { mutableStateOf(false) }

    var typeExpanded by remember { mutableStateOf(false) }
    var permExpanded by remember { mutableStateOf(false) }

    val highlighter = remember { ShellSyntaxHighlighter() }

    val fileTypes = remember {
        listOf(
            "text"   to context.getString(R.string.file_type_text),
            "script" to context.getString(R.string.file_type_script),
            "binary" to context.getString(R.string.file_type_binary),
            "config" to context.getString(R.string.file_type_config)
        )
    }
    val permissions = remember {
        listOf(
            "0644" to "0644 - ${context.getString(R.string.permission_regular)}",
            "0755" to "0755 - ${context.getString(R.string.permission_executable)}",
            "0700" to "0700 - ${context.getString(R.string.permission_owner_only)}",
            "0600" to "0600 - ${context.getString(R.string.permission_read_write)}"
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val displayName = getDisplayName(context, uri)
                viewModel.addFileFromUri(context, uri, displayName)
            }
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snack_files_added, uris.size)) }
        }
    }

    fun addOrUpdateFile() {
        if (fileName.isBlank()) return
        val newFile = ModuleFile(
            name = fileName, content = fileContent,
            permissions = filePermissions, type = fileType
        )
        val index = editingIndex
        if (index != null && index in viewModel.moduleFiles.indices) {
            viewModel.moduleFiles[index] = newFile
            editingIndex = null
        } else {
            viewModel.moduleFiles.add(newFile)
        }
        fileName = ""; fileContent = ""; filePermissions = "0644"; fileType = "text"
        showAddForm = false
    }

    fun onFileNameChange(name: String) {
        fileName = name
        if (editingIndex == null) {
            val lower = name.lowercase()
            val ext = lower.substringAfterLast(".", "")
            when {
                ext in ModuleViewModel.EXT_SCRIPTS -> { fileType = "script"; filePermissions = "0755" }
                ext in ModuleViewModel.EXT_CONFIG  -> { fileType = "config"; filePermissions = "0644" }
                ext in ModuleViewModel.EXT_BINARY  -> { fileType = "binary"; filePermissions = "0755" }
                ModuleViewModel.PATH_EXECUTABLE.any { lower.contains(it) } -> { filePermissions = "0755" }
            }
        }
    }

    fun startEdit(index: Int) {
        val file = viewModel.moduleFiles[index]
        fileName = file.name; fileContent = file.content
        filePermissions = file.permissions; fileType = file.type
        editingIndex = index; showAddForm = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Скрипты ──────────────────────────────────────────────────────────
        // [+] Скрипты теперь в единой вкладке вместе с файлами оверлея
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.section_scripts), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        }
        Spacer(Modifier.height(8.dp))

        CollapsibleScriptCard(
            name = "customize.sh", required = true,
            script = viewModel.customizeScript,
            onScriptChange = { viewModel.customizeScript = it },
            onFillDefault = { viewModel.fillDefaultCustomize() },
            snippetTarget = "customize", viewModel = viewModel,
            visualTransformation = highlighter
        )
        Spacer(Modifier.height(8.dp))
        CollapsibleScriptCard(
            name = "service.sh", required = false,
            script = viewModel.serviceScript,
            onScriptChange = { viewModel.serviceScript = it },
            onFillDefault = { viewModel.fillDefaultService() },
            snippetTarget = "service", viewModel = viewModel,
            visualTransformation = highlighter
        )
        Spacer(Modifier.height(8.dp))
        CollapsibleScriptCard(
            name = "post-fs-data.sh", required = false,
            script = viewModel.postFsScript,
            onScriptChange = { viewModel.postFsScript = it },
            onFillDefault = { viewModel.fillDefaultPostFs() },
            snippetTarget = null, viewModel = null,
            visualTransformation = highlighter
        )

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = Border)
        Spacer(Modifier.height(16.dp))

        // ── Файлы оверлея ─────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.section_overlay_files), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            // [+] Подписи под иконками — теперь понятно что делает каждая кнопка
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                    Icon(AppIcons.UploadFile, contentDescription = stringResource(R.string.btn_upload_label), tint = Accent)
                }
                Text(stringResource(R.string.btn_upload_label), fontSize = 9.sp, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    showAddForm = !showAddForm
                    if (!showAddForm && editingIndex != null) {
                        editingIndex = null
                        fileName = ""; fileContent = ""; filePermissions = "0644"; fileType = "text"
                    }
                }) {
                    Icon(
                        if (showAddForm) Icons.Default.KeyboardArrowUp else Icons.Default.Add,
                        contentDescription = stringResource(R.string.btn_add_file),
                        tint = Accent
                    )
                }
                Text(if (showAddForm) stringResource(R.string.btn_hide_label) else stringResource(R.string.btn_create_label), fontSize = 9.sp, color = TextMuted)
            }
            if (viewModel.moduleFiles.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { viewModel.moduleFiles.clear() }) {
                        Icon(AppIcons.DeleteSweep, contentDescription = stringResource(R.string.btn_clear_label), tint = Danger)
                    }
                    Text(stringResource(R.string.btn_clear_label), fontSize = 9.sp, color = Danger.copy(alpha = 0.7f))
                }
            }
        }

        // Форма добавления/редактирования (скрывается когда не нужна)
        AnimatedVisibility(
            visible = showAddForm,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(tween(200)),
            exit  = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(200))
        ) {
            Spacer(Modifier.height(8.dp))
            FileAddForm(
                fileName = fileName, onFileNameChange = { onFileNameChange(it) },
                fileContent = fileContent, onFileContentChange = { fileContent = it },
                filePermissions = filePermissions, onFilePermissionsChange = { filePermissions = it },
                fileType = fileType, onFileTypeChange = { fileType = it },
                typeExpanded = typeExpanded, onTypeExpandedChange = { typeExpanded = it },
                permExpanded = permExpanded, onPermExpandedChange = { permExpanded = it },
                fileTypes = fileTypes, permissions = permissions,
                editingIndex = editingIndex,
                onAddOrUpdate = { addOrUpdateFile() },
                onPickFile = { filePickerLauncher.launch("*/*") }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Список файлов
        FileListPanel(
            files = viewModel.moduleFiles,
            onEdit = { startEdit(it) },
            onDelete = { viewModel.moduleFiles.removeAt(it) },
            onClearAll = { viewModel.moduleFiles.clear() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileAddForm(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    fileContent: String,
    onFileContentChange: (String) -> Unit,
    filePermissions: String,
    onFilePermissionsChange: (String) -> Unit,
    fileType: String,
    onFileTypeChange: (String) -> Unit,
    typeExpanded: Boolean,
    onTypeExpandedChange: (Boolean) -> Unit,
    permExpanded: Boolean,
    onPermExpandedChange: (Boolean) -> Unit,
    fileTypes: List<Pair<String, String>>,
    permissions: List<Pair<String, String>>,
    editingIndex: Int?,
    onAddOrUpdate: () -> Unit,
    onPickFile: () -> Unit
) {
    val highlighter = remember { ShellSyntaxHighlighter() }
    val applyHighlight = fileType == "script" || fileName.endsWith(".sh", ignoreCase = true)
    // [*] remember — поиск по списку не повторяется при каждой рекомпозиции
    val selectedTypeLabel = remember(fileType, fileTypes) {
        fileTypes.firstOrNull { it.first == fileType }?.second ?: fileType
    }
    val selectedPermLabel = remember(filePermissions, permissions) {
        permissions.firstOrNull { it.first == filePermissions }?.second ?: filePermissions
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.title_add_file), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
            	shape = TextFieldShape,
                value = fileName,
                onValueChange = onFileNameChange,
                label = { Text(stringResource(R.string.label_file_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = onTypeExpandedChange) {
                OutlinedTextField(
                	shape = TextFieldShape,
                    value = selectedTypeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_file_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = textFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { onTypeExpandedChange(false) }
                ) {
                    fileTypes.forEach { (key, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { onFileTypeChange(key) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(expanded = permExpanded, onExpandedChange = onPermExpandedChange) {
                OutlinedTextField(
                	shape = TextFieldShape,
                    value = selectedPermLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_permissions)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = permExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = textFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = permExpanded,
                    onDismissRequest = { onPermExpandedChange(false) }
                ) {
                    permissions.forEach { (key, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { onFilePermissionsChange(key) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
            	shape = TextFieldShape,
                value = fileContent,
                onValueChange = onFileContentChange,
                label = { Text(stringResource(R.string.label_content)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                colors = textFieldColors(),
                visualTransformation = if (applyHighlight) highlighter else VisualTransformation.None
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAddOrUpdate,
                modifier = Modifier.fillMaxWidth().background(BrandGradient, ButtonDefaults.shape),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text(
                    if (editingIndex != null) stringResource(R.string.btn_save_changes) else stringResource(R.string.btn_add_file),
                    color = Color.White, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
                Icon(AppIcons.UploadFile, contentDescription = null, tint = TextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_upload_file), color = TextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileListPanel(
    files: List<ModuleFile>,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.title_file_structure), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(12.dp), color = Accent.copy(alpha = 0.15f)) {
                    Text(
                        stringResource(R.string.files_count, files.size),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        color = Accent
                    )
                }
                if (files.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showClearAllDialog = true }) {
                        Icon(AppIcons.DeleteSweep, contentDescription = stringResource(R.string.btn_clear_all), tint = Danger)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📂", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.no_files_added), color = TextSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.hint_add_files),
                            color = TextMuted, fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.hint_add_files_how),
                            color = TextMuted, fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
    modifier = Modifier.heightIn(max = 400.dp)
) {
    // [*] Составной ключ name+index — уникален даже при дублях имён
    itemsIndexed(files, key = { index, file -> "${file.name}_$index" }) { index, file ->
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                    deleteIndex = index
                }
                // [*] false — элемент всегда снэпбэкает, нет визуального глюка при отмене
                false
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF5252)
                    else -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                        Icon(Icons.Default.Delete, stringResource(R.string.btn_delete), tint = Color.White)
                }
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            modifier = Modifier.animateItem()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                FileItemWithPreview(
                    file = file,
                    onEdit = { onEdit(index) },
                    onDeleteRequest = { deleteIndex = index }
                )
            }
        }
        HorizontalDivider(color = Border.copy(alpha = 0.2f))
    }
}
            }
        }
    }

    deleteIndex?.let { index ->
        // [*] getOrNull — защита от IndexOutOfBoundsException при изменении списка
        val fileName = files.getOrNull(index)?.name ?: return@let
        AlertDialog(
            onDismissRequest = { deleteIndex = null },
            title = { Text(stringResource(R.string.dlg_delete_file_title)) },
            text = { Text(stringResource(R.string.dlg_delete_file_message, fileName)) },
            confirmButton = {
                TextButton(onClick = { onDelete(index); deleteIndex = null }) { Text(stringResource(R.string.dlg_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteIndex = null }) { Text(stringResource(R.string.dlg_delete_cancel)) }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.dlg_clear_all_title)) },
            text = { Text(stringResource(R.string.dlg_clear_all_message, files.size)) },
            confirmButton = {
                TextButton(onClick = { onClearAll(); showClearAllDialog = false }) { Text(stringResource(R.string.dlg_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text(stringResource(R.string.dlg_clear_cancel)) }
            }
        )
    }
}

@Composable
private fun FileItemWithPreview(
    file: ModuleFile,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // [*] remember — иконка не пересчитывается при каждой рекомпозиции
    val icon = remember(file.name) { getFileIcon(file.name) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Row {
                    val permColor = if (file.permissions == "0755") Success else TextMuted
                    Text("${file.permissions} · ${file.type}", color = permColor, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.btn_edit), tint = TextSecondary)
            }
            IconButton(onClick = onDeleteRequest) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = Danger)
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(200))
        ) {
            if (file.isBinary) {
                Text(
                    text = "[binary data]",
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp).fillMaxWidth(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            } else {
                Text(
                    text = file.content.ifBlank { "// empty" },
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp).fillMaxWidth(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

fun getFileIcon(fileName: String): String = when {
    fileName.endsWith(".sh") -> "📜"
    fileName.endsWith(".prop") -> "📄"
    fileName.endsWith(".xml") -> "📰"
    fileName.endsWith(".conf") || fileName.endsWith(".cfg") -> "⚙️"
    fileName.endsWith(".so") -> "🔧"
    fileName.endsWith(".bin") -> "💾"
    fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".svg") -> "🖼️"
    fileName.endsWith(".db") || fileName.endsWith(".sqlite") -> "🗃️"
    else -> "📄"
}

// [+] Правильное получение имени файла из content:// URI через ContentResolver.
//     uri.lastPathSegment возвращает document ID, а не отображаемое имя.
private fun getDisplayName(context: android.content.Context, uri: android.net.Uri): String {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (col >= 0 && cursor.moveToFirst()) cursor.getString(col) else null
        } ?: uri.lastPathSegment?.substringAfterLast("/") ?: "unknown"
    } catch (_: Exception) {
        uri.lastPathSegment?.substringAfterLast("/") ?: "unknown"
    }
}