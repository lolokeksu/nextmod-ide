package com.magisk.next.ui.tabs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magisk.next.R
import com.magisk.next.ui.theme.*
import com.magisk.next.viewmodel.ModuleViewModel
import com.magisk.next.viewmodel.ValidationItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PreviewTab(viewModel: ModuleViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isWide = LocalConfiguration.current.screenWidthDp >= 600

    var validationResults by remember { mutableStateOf<List<ValidationItem>>(emptyList()) }
    var isValidationRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(300))

    // [*] derivedStateOf — отслеживает изменения состояний ViewModel
    val rawModuleProp by remember { derivedStateOf { viewModel.generateModuleProp() } }
    val rawTreeText   by remember { derivedStateOf { buildTreePreview(viewModel) } }
    val totalSize     by remember { derivedStateOf { viewModel.getTotalSize() } }
    val totalFiles    by remember {
        derivedStateOf {
            viewModel.moduleFiles.size + listOfNotNull(
                viewModel.customizeScript.ifBlank { null },
                viewModel.serviceScript.ifBlank { null },
                viewModel.postFsScript.ifBlank { null }
            ).count()
        }
    }

    // [+] Debounce 500мс — дерево и module.prop не пересчитываются при каждом символе,
    //     а обновляются через полсекунды после последнего изменения
    var moduleProp by remember { mutableStateOf(rawModuleProp) }
    var treeText   by remember { mutableStateOf(rawTreeText) }
    var isUpdating by remember { mutableStateOf(false) }

    LaunchedEffect(rawModuleProp) {
        isUpdating = true
        delay(500)
        moduleProp = rawModuleProp
        isUpdating = false
    }
    LaunchedEffect(rawTreeText) {
        isUpdating = true
        delay(500)
        treeText = rawTreeText
        isUpdating = false
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (isWide) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    // [*] initiallyExpanded = true — сразу видно содержимое
                    CollapsibleCard(title = stringResource(R.string.title_module_prop), initiallyExpanded = true) {
                        ModulePropContent(moduleProp) {
                            scope.launch {
                                val uri = viewModel.saveModuleProp(context)
                                if (uri != null) {
                                    Toast.makeText(context, context.getString(R.string.toast_module_prop_saved), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    // [+] Индикатор обновления в заголовке
                    val structureTitle = if (isUpdating)
                        "${stringResource(R.string.title_module_structure)} ●"
                    else stringResource(R.string.title_module_structure)
                    CollapsibleCard(title = structureTitle, initiallyExpanded = true) {
                        StructureContent(treeText, totalSize, totalFiles)
                    }
                }
            }
        } else {
            CollapsibleCard(title = stringResource(R.string.title_module_prop), initiallyExpanded = true) {
                ModulePropContent(moduleProp) {
                    scope.launch {
                        val uri = viewModel.saveModuleProp(context)
                        if (uri != null) {
                            Toast.makeText(context, context.getString(R.string.toast_module_prop_saved), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val structureTitle = if (isUpdating)
                "${stringResource(R.string.title_module_structure)} ●"
            else stringResource(R.string.title_module_structure)
            CollapsibleCard(title = structureTitle, initiallyExpanded = true) {
                StructureContent(treeText, totalSize, totalFiles)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CollapsibleCard(title = stringResource(R.string.title_validation), initiallyExpanded = false) {
            ValidationContent(
                isRunning = isValidationRunning,
                progress = animatedProgress,
                results = validationResults,
                onRunValidation = {
                    isValidationRunning = true
                    progress = 0f
                    scope.launch {
                        while (progress < 0.9f) {
                            delay(50)
                            progress += 0.05f
                        }
                        validationResults = viewModel.validate(context)
                        progress = 1f
                        delay(200)
                        isValidationRunning = false
                    }
                }
            )
        }
    }
}

@Composable
private fun CollapsibleCard(
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.collapse_section) else stringResource(R.string.expand_section),
                    tint = TextSecondary
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(tween(300))
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ModulePropContent(moduleProp: String, onDownload: () -> Unit) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = BgTertiary) {
            Text(
                text = moduleProp.ifBlank { "id=...\nname=...\nversion=..." },
                fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextPrimary,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onDownload) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(brush = BrandGradient)) {
                        append(stringResource(R.string.btn_download_prop))
                    }
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StructureContent(treeText: String, totalSize: Long, totalFiles: Int) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Surface(modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), shape = RoundedCornerShape(8.dp), color = BgTertiary) {
            Text(
                text = treeText.ifBlank { "module/\n├── module.prop\n..." },
                fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextPrimary,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("📦 ${formatSize(totalSize)}", color = TextSecondary, fontSize = 12.sp)
            Text("📄 $totalFiles ${stringResource(R.string.files_word)}", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ValidationContent(
    isRunning: Boolean, progress: Float, results: List<ValidationItem>,
    onRunValidation: () -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        TextButton(
            onClick = onRunValidation, enabled = !isRunning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(brush = if (!isRunning) BrandGradient else SolidColor(TextSecondary))) {
                        append(if (isRunning) stringResource(R.string.validation_running) else stringResource(R.string.btn_validate))
                    }
                },
                fontWeight = FontWeight.SemiBold
            )
        }

        if (isRunning) {
            Spacer(modifier = Modifier.height(16.dp))
            ShimmerBlock(modifier = Modifier.fillMaxWidth().height(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBlock(modifier = Modifier.fillMaxWidth(0.8f).height(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBlock(modifier = Modifier.fillMaxWidth(0.6f).height(20.dp))
        }

        if (results.isNotEmpty() && !isRunning) {
            Spacer(modifier = Modifier.height(12.dp))
            val passed = results.count { it.status == "success" }
            val total = results.size
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.result_checks, passed, total), color = TextSecondary, fontSize = 13.sp)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (passed == total) Success.copy(alpha = 0.15f) else Warning.copy(alpha = 0.15f)
                ) {
                    Text(
                        if (passed == total) stringResource(R.string.validation_passed) else stringResource(R.string.validation_warnings),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp,
                        color = if (passed == total) Success else Warning
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            results.forEach { item ->
                val (icon, color) = when (item.status) {
                    "success" -> "✅" to Success
                    "error" -> "❌" to Danger
                    "warning" -> "⚠️" to Warning
                    else -> "ℹ️" to TextSecondary
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(item.message, color = color, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ShimmerBlock(modifier: Modifier = Modifier) {
    // [*] remember — список цветов не пересоздаётся при каждой рекомпозиции
    val shimmerColors = remember {
        listOf(
            Color.LightGray.copy(alpha = 0.1f),
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.1f)
        )
    }
    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 200, 0f),
        end = Offset(translateAnim.value, 0f)
    )
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp)).background(brush))
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun buildTreePreview(viewModel: ModuleViewModel): String {
    val sb = StringBuilder()
    val moduleName = viewModel.moduleId.ifBlank { "module" }
    sb.appendLine("$moduleName/")
    sb.appendLine("├── module.prop")
    if (viewModel.customizeScript.isNotBlank()) sb.appendLine("├── customize.sh")
    if (viewModel.serviceScript.isNotBlank()) sb.appendLine("├── service.sh")
    if (viewModel.postFsScript.isNotBlank()) sb.appendLine("├── post-fs-data.sh")

    sb.appendLine("├── META-INF/")
    sb.appendLine("│   └── com/")
    sb.appendLine("│       └── google/")
    sb.appendLine("│           └── android/")
    sb.appendLine("│               ├── update-binary")
    sb.appendLine("│               └── updater-script")

    if (viewModel.sepolicyRules.isNotBlank()) sb.appendLine("├── sepolicy.rule")

    val replacePaths = viewModel.replaceFiles.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (replacePaths.isNotEmpty()) {
        sb.appendLine("├── system/")
        for ((i, path) in replacePaths.withIndex()) {
            val isLast = (i == replacePaths.lastIndex) && viewModel.moduleFiles.isEmpty()
            val prefix = if (isLast) "│   └── " else "│   ├── "
            sb.appendLine("$prefix$path")
        }
        if (viewModel.moduleFiles.isNotEmpty()) {
            sb.appendLine("│   ⋮")
        }
    }

    if (viewModel.moduleFiles.isNotEmpty()) {
        val paths = viewModel.moduleFiles.map { it.name }.sorted()
        data class Node(val name: String, val isFile: Boolean) {
            val children = mutableListOf<Node>()
        }
        val root = Node("", false)
        for (fullPath in paths) {
            val parts = fullPath.split("/").filter { it.isNotEmpty() }
            var current = root
            for ((i, part) in parts.withIndex()) {
                val isFile = (i == parts.lastIndex)
                var child = current.children.find { it.name == part && it.isFile == isFile }
                if (child == null) {
                    child = Node(part, isFile)
                    current.children.add(child)
                }
                current = child
            }
        }

        fun render(node: Node, indent: String, isLastSibling: Boolean) {
            for ((i, child) in node.children.withIndex()) {
                val lastChild = (i == node.children.lastIndex)
                val prefix = if (lastChild) "└── " else "├── "
                val line = "$indent$prefix${child.name}${if (!child.isFile) "/" else ""}"
                sb.appendLine(line)
                if (!child.isFile) {
                    val nextIndent = indent + if (lastChild) "    " else "│   "
                    render(child, nextIndent, lastChild)
                }
            }
        }

        val rootChildren = root.children
        if (rootChildren.isNotEmpty()) {
            val firstChild = rootChildren.first()
            val firstPrefix = "├── "
            sb.appendLine("$firstPrefix${firstChild.name}${if (!firstChild.isFile) "/" else ""}")
            if (!firstChild.isFile) {
                render(firstChild, "│   ", false)
            }
            for (i in 1 until rootChildren.size) {
                val child = rootChildren[i]
                val last = (i == rootChildren.lastIndex)
                val prefix = if (last) "└── " else "├── "
                sb.appendLine("$prefix${child.name}${if (!child.isFile) "/" else ""}")
                if (!child.isFile) {
                    val childIndent = if (last) "    " else "│   "
                    render(child, childIndent, last)
                }
            }
        }
    }

    return sb.toString().trimEnd()
}