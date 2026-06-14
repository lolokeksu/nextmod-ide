package com.magisk.next

import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.magisk.next.root.RootManager
import com.magisk.next.ui.BrandSplashScreen
import com.magisk.next.ui.LocaleHelper
import com.magisk.next.ui.MainScreen
import com.magisk.next.ui.theme.MagiskModuleBuilderTheme
import com.magisk.next.viewmodel.ModuleViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable

class MainActivity : ComponentActivity() {

    private lateinit var appSettings: AppSettings
    private val moduleViewModel: ModuleViewModel by viewModels()

    override fun attachBaseContext(newBase: Context?) {
        val context = newBase ?: baseContext
        val wrappedContext = LocaleHelper.applySavedLocale(context)
        super.attachBaseContext(wrappedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [+] Перехват крашей — первым делом, до любой другой инициализации
        CrashHandler.install(this)

        // [+] Отчёт о сбое с прошлого запуска (null, если сбоя не было)
        val pendingCrash = CrashHandler.getPendingCrashLog(this)

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    .build()
            )
        }

        appSettings = AppSettings(this.applicationContext)
        Logger.setLevel(appSettings.logLevel)
        if (appSettings.rootEnabled) {
            RootManager.init(debug = BuildConfig.DEBUG)
        }

        // [+] Онбординг при первом запуске — применяем шаблон чтобы пользователь
        //     увидел заполненные поля вместо пустого экрана
        if (appSettings.isFirstLaunch) {
            moduleViewModel.applyTemplate(com.magisk.next.viewmodel.ModuleTemplate.EMPTY)
            appSettings.isFirstLaunch = false
        }

        setContent {
            MagiskModuleBuilderTheme {
                MainScreen(viewModel = moduleViewModel)

                // [+] Диалог отчёта о сбое — поверх основного экрана
                var crashLog by remember { mutableStateOf(pendingCrash) }
                crashLog?.let { log ->
                    CrashReportDialog(
                        log = log,
                        onDismiss = {
                            CrashHandler.markCrashSeen(applicationContext)
                            crashLog = null
                        }
                    )
                }

                // [+] Брендовый сплеш при запуске — поверх всего
                var showSplash by rememberSaveable { mutableStateOf(true) }
                if (showSplash) {
                    BrandSplashScreen(onFinished = { showSplash = false }, durationMillis = 2000L)
                }
            }
        }

        // [+] Периодическое автосохранение каждые 3 минуты
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(3 * 60 * 1000L)
                    if (appSettings.autoSaveOnExit) {
                        try {
                            val autosaveFile = java.io.File(cacheDir, "autosave.mmproj")
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                this@MainActivity,
                                "${packageName}.fileprovider",
                                autosaveFile
                            )
                            moduleViewModel.saveProject(this@MainActivity, uri) { success ->
                                if (success) Logger.logInfo("Periodic autosave OK")
                                else Logger.logError("Periodic autosave failed")
                            }
                        } catch (e: Exception) {
                            Logger.logError("Periodic autosave error: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (appSettings.autoSaveOnExit) {
            lifecycleScope.launch {
                try {
                    val autosaveFile = java.io.File(cacheDir, "autosave.mmproj")
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this@MainActivity,
                        "${packageName}.fileprovider",
                        autosaveFile
                    )
                    moduleViewModel.saveProject(this@MainActivity, uri) { success ->
                        if (!success) {
                            Logger.logError("Autosave failed")
                        } else {
                            Logger.logInfo("Autosave completed")
                        }
                    }
                } catch (e: Exception) {
                    Logger.logError("Autosave failed: ${e.message}")
                }
            }
        }
    }
}

/**
 * Диалог отчёта о сбое прошлого запуска.
 * Показывает превью лога (до 2000 символов), в буфер копируется полный текст.
 */
@Composable
private fun CrashReportDialog(log: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.crash_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.crash_dialog_message))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = log.take(2000),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(log))
                Toast.makeText(
                    context,
                    context.getString(R.string.crash_dialog_copied),
                    Toast.LENGTH_SHORT
                ).show()
                onDismiss()
            }) { Text(stringResource(R.string.crash_dialog_copy)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.crash_dialog_close)) }
        }
    )
}