package com.magisk.next.ui.tabs

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import com.magisk.next.ui.theme.Accent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magisk.next.R
import com.magisk.next.ui.theme.*
import com.magisk.next.viewmodel.ModuleViewModel

@Composable
fun AdvancedTab(viewModel: ModuleViewModel) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // [*] Объединено: опции модуля + специальные файлы в одной карточке.
        //     Убраны устаревшие флаги systemless/needsystem/recovery_mode.
        CollapsibleCard(title = stringResource(R.string.title_options), initiallyExpanded = true) {
            OptionsContent(viewModel)
        }

        Spacer(modifier = Modifier.height(16.dp))
        CollapsibleCard(title = stringResource(R.string.title_summary), initiallyExpanded = false) {
            SummaryContent(viewModel)
        }

        // [+] Update-binary перенесён из вкладки Скрипты
        Spacer(modifier = Modifier.height(16.dp))
        CollapsibleCard(title = stringResource(R.string.title_update_binary), initiallyExpanded = false) {
            UpdateBinaryContent(viewModel)
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
private fun OptionsContent(viewModel: ModuleViewModel) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        OutlinedTextField(
        	shape = TextFieldShape,
            value = viewModel.minMagisk,
            onValueChange = { viewModel.minMagisk = it },
            label = { Text(stringResource(R.string.label_min_magisk)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // [*] Оставлен только актуальный флаг skip_mount.
        //     systemless / needsystem / recovery_mode — устаревшие, убраны.
        SwitchOption(stringResource(R.string.option_skip_mount), stringResource(R.string.desc_skip_mount), viewModel.skipMount) {
            viewModel.skipMount = it
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Border)
        Spacer(modifier = Modifier.height(16.dp))

        // [*] Специальные файлы перенесены сюда из отдельной карточки
        OutlinedTextField(
        	shape = TextFieldShape,
            value = viewModel.replaceFiles,
            onValueChange = { viewModel.replaceFiles = it },
            label = { Text(stringResource(R.string.label_replace)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
            colors = textFieldColors()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(stringResource(R.string.hint_replace), color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
        	shape = TextFieldShape,
            value = viewModel.sepolicyRules,
            onValueChange = { viewModel.sepolicyRules = it },
            label = { Text(stringResource(R.string.label_sepolicy)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
            colors = textFieldColors()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(stringResource(R.string.hint_sepolicy), color = TextMuted, fontSize = 12.sp)

        // [+] Автообновление (updateJson)
        Spacer(modifier = Modifier.height(16.dp))
        SwitchOption(
            stringResource(R.string.adv_updatejson_title),
            stringResource(R.string.adv_updatejson_desc),
            viewModel.updateJsonEnabled
        ) { viewModel.updateJsonEnabled = it }

        if (viewModel.updateJsonEnabled) {
            val ctx = LocalContext.current
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                shape = TextFieldShape,
                value = viewModel.updateJsonUrl,
                onValueChange = { viewModel.updateJsonUrl = it },
                label = { Text(stringResource(R.string.adv_updatejson_url)) },
                placeholder = { Text(stringResource(R.string.adv_updatejson_url_hint), fontSize = 11.sp, color = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                colors = textFieldColors()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                shape = TextFieldShape,
                value = viewModel.updateZipUrl,
                onValueChange = { viewModel.updateZipUrl = it },
                label = { Text(stringResource(R.string.adv_updatezip_url)) },
                placeholder = { Text(stringResource(R.string.adv_updatezip_url_hint), fontSize = 11.sp, color = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                colors = textFieldColors()
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = {
                try {
                    val dir = java.io.File(android.os.Environment.getExternalStorageDirectory(), "MagiskModuleCreator")
                    if (!dir.exists()) dir.mkdirs()
                    java.io.File(dir, "update.json").writeText(viewModel.generateUpdateJson())
                    android.widget.Toast.makeText(ctx, ctx.getString(R.string.toast_updatejson_saved), android.widget.Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
            }) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(brush = BrandGradient)) {
                            append(stringResource(R.string.adv_generate_updatejson))
                        }
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SwitchOption(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, color = TextMuted, fontSize = 12.sp)
        }
        GradientSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SummaryContent(viewModel: ModuleViewModel) {
    // [*] derivedStateOf — пересчёт только при изменении зависимых состояний
    val totalFiles  by remember { derivedStateOf { viewModel.moduleFiles.size } }
    val scriptCount by remember {
        derivedStateOf {
            listOf(viewModel.customizeScript, viewModel.serviceScript, viewModel.postFsScript)
                .count { it.isNotBlank() }
        }
    }
    val totalSize by remember { derivedStateOf { viewModel.getTotalSize() } }
    val version   by remember { derivedStateOf { viewModel.moduleVersion.ifBlank { "-" } } }

    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SummaryItem(stringResource(R.string.summary_files), totalFiles.toString(), Accent)
            SummaryItem(stringResource(R.string.summary_scripts), scriptCount.toString(), Success)
            SummaryItem(stringResource(R.string.summary_size), formatSize(totalSize), Purple)
            SummaryItem(stringResource(R.string.summary_version), version, Warning)
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun GradientSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    // [*] Кастомный трек: градиент бренда при включении, тёмный при выключении
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) BrandGradient else SolidColor(BgTertiary))
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Composable
private fun UpdateBinaryContent(viewModel: ModuleViewModel) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = viewModel.updateBinaryType == "symlink",
                onClick = { viewModel.updateBinaryType = "symlink" },
                colors = RadioButtonDefaults.colors(selectedColor = Accent)
            )
            Text(stringResource(R.string.label_symlink_recommended), color = TextSecondary)
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = viewModel.updateBinaryType == "custom",
                onClick = { viewModel.updateBinaryType = "custom" },
                colors = RadioButtonDefaults.colors(selectedColor = Accent)
            )
            Text(stringResource(R.string.label_custom), color = TextSecondary)
        }
        if (viewModel.updateBinaryType == "custom") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
            	shape = TextFieldShape,
                value = viewModel.updateBinaryCustom,
                onValueChange = { viewModel.updateBinaryCustom = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                colors = textFieldColors()
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = BgTertiary
            ) {
                Text(
                    stringResource(R.string.using_symlink),
                    modifier = Modifier.padding(12.dp),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}