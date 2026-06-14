package com.magisk.next.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.magisk.next.root.InstalledModule
import com.magisk.next.root.RootManager
import com.magisk.next.root.RootStatus
import com.magisk.next.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var modules by remember { mutableStateOf<List<InstalledModule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var rootStatus by remember { mutableStateOf(RootStatus.UNAVAILABLE) }
    var moduleToRemove by remember { mutableStateOf<InstalledModule?>(null) }

    // Загружаем список модулей при открытии
    suspend fun reload() {
        isLoading = true
        rootStatus = RootManager.getStatus()
        modules = if (rootStatus == RootStatus.GRANTED) {
            RootManager.getInstalledModules()
        } else emptyList()
        isLoading = false
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_module_manager), color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { reload() } }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_refresh), tint = Accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent)
                    }
                }

                rootStatus != RootStatus.GRANTED -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.no_root_access),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.hint_enable_root),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                modules.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_modules_installed), color = TextSecondary)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(modules, key = { it.id }) { module ->
                            ModuleCard(
                                module = module,
                                onToggle = { enabled ->
                                    scope.launch {
                                        val ok = RootManager.setModuleEnabled(module.id, enabled)
                                        if (ok) {
                                            modules = modules.map {
                                                if (it.id == module.id) it.copy(isEnabled = enabled) else it
                                            }
                                        }
                                    }
                                },
                                onRemove = { moduleToRemove = module }
                            )
                        }
                    }
                }
            }
        }
    }

    // Диалог подтверждения удаления
    moduleToRemove?.let { module ->
        AlertDialog(
            onDismissRequest = { moduleToRemove = null },
            icon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) },
            title = { Text(stringResource(R.string.dlg_remove_module_title)) },
            text = {
                Text(
                    stringResource(R.string.dlg_remove_module_message, module.name),
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ok = RootManager.removeModule(module.id)
                        if (ok) {
                            modules = modules.map {
                                if (it.id == module.id) it.copy(isMarkedForRemoval = true) else it
                            }
                        }
                        moduleToRemove = null
                    }
                }) { Text(stringResource(R.string.dlg_delete_confirm), color = Color(0xFFEF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { moduleToRemove = null }) { Text(stringResource(R.string.dlg_delete_cancel)) }
            }
        )
    }
}

@Composable
private fun ModuleCard(
    module: InstalledModule,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (module.isMarkedForRemoval) Color(0xFF2A1515) else BgSecondary
        ),
        border = BorderStroke(
            1.dp,
            when {
                module.isMarkedForRemoval -> Color(0xFFEF4444)
                module.isEnabled          -> Accent.copy(alpha = 0.4f)
                else                      -> Border
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        module.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${module.id} · v${module.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (!module.isMarkedForRemoval) {
                    Switch(
                        checked = module.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandCyan)
                    )
                }
            }

            if (module.author.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.label_author) + ": ${module.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            if (module.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 3
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Статус-метка
                val (statusText, statusColor) = when {
                    module.isMarkedForRemoval -> stringResource(R.string.module_marked_removal) to Color(0xFFEF4444)
                    module.isEnabled          -> stringResource(R.string.module_enabled)        to Color(0xFF10B981)
                    else                      -> stringResource(R.string.module_disabled)       to TextMuted
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(statusText, color = statusColor, style = MaterialTheme.typography.labelSmall)
                }

                Spacer(Modifier.weight(1f))

                if (!module.isMarkedForRemoval) {
                    TextButton(
                        onClick = onRemove,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_delete))
                    }
                }
            }
        }
    }
}