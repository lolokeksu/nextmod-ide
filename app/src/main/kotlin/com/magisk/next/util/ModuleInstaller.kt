package com.magisk.next.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.magisk.next.R
import com.magisk.next.root.RootInstallResult
import com.magisk.next.root.RootManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Установка собранного модуля.
 * Вынесено из MainScreen для разгрузки UI-файла.
 */

/** Установка через Intent (ACTION_VIEW) с автоподбором root-менеджера. */
fun installModule(context: Context, uri: Uri) {
    val managers = listOf(
        "com.topjohnwu.magisk",
        "me.weishu.kernelsu",
        "me.bmax.apatch"
    )

    val pm = context.packageManager
    val installed = managers.firstOrNull {
        try { pm.getPackageInfo(it, 0); true } catch (_: Exception) { false }
    }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/zip")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (installed != null) setPackage(installed)
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.menu_install_module)))
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.toast_no_installer),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

/**
 * Прямая root-установка через RootManager — без Intent, без выбора приложения.
 * Быстрее и надёжнее: magisk/ksud/apd --install-module напрямую.
 */
fun installModuleWithRoot(
    scope: CoroutineScope,
    context: Context,
    zipPath: String
) {
    scope.launch {
        Toast.makeText(context, context.getString(R.string.toast_installing), Toast.LENGTH_SHORT).show()
        when (val result = RootManager.installModule(zipPath)) {
            is RootInstallResult.Success ->
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_module_installed, result.manager),
                    Toast.LENGTH_LONG
                ).show()
            is RootInstallResult.Error ->
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_install_error, result.message),
                    Toast.LENGTH_LONG
                ).show()
        }
    }
}

/**
 * Вычисляет путь к ZIP-файлу из параметров модуля.
 * Совпадает с логикой ModuleBuilder.buildAndSaveZip.
 */
fun getZipFilePath(moduleId: String, moduleVersion: String): String {
    val name = "${moduleId.ifBlank { "template_module" }}-${moduleVersion.ifBlank { "1.0.0" }}.zip"
    return "${Environment.getExternalStorageDirectory().absolutePath}/MagiskModuleCreator/Project/$name"
}
