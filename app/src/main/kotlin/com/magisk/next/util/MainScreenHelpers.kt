package com.magisk.next.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.magisk.next.AppSettings
import com.magisk.next.R
import java.io.File

/**
 * Вспомогательные функции экрана MainScreen.
 * Вынесено для разгрузки UI-файла.
 */

/** Форматирование размера файла для превью структуры ZIP. */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val group = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return "%.1f %s".format(bytes / Math.pow(1024.0, group.toDouble()), units[group])
}

/** Извлекает путь к файлу проекта из URI (только для файлов в MagiskModuleCreator/Project/). */
fun resolveProjectPath(context: Context, uri: Uri): Pair<String, String>? {
    return try {
        val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        } ?: return null
        val projectDir = File(Environment.getExternalStorageDirectory(), "MagiskModuleCreator/Project")
        val file = File(projectDir, displayName)
        if (file.exists()) Pair(file.absolutePath, file.nameWithoutExtension) else null
    } catch (_: Exception) { null }
}

/** Открывает проект из списка недавних через FileProvider. */
fun openRecentProject(
    context: Context,
    project: AppSettings.RecentProject,
    appSettings: AppSettings,
    onLoaded: (Uri) -> Unit,
    onNotFound: () -> Unit
) {
    val file = File(project.path)
    if (!file.exists()) { onNotFound(); return }
    val uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )
    onLoaded(uri)
}

/** Форматирует время относительно текущего момента. */
fun formatRelativeTime(context: Context, timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L       -> context.getString(R.string.time_just_now)
        diff < 3_600_000L    -> context.getString(R.string.time_min_ago, diff / 60_000)
        diff < 86_400_000L   -> context.getString(R.string.time_hours_ago, diff / 3_600_000)
        diff < 172_800_000L  -> context.getString(R.string.time_yesterday)
        diff < 604_800_000L  -> context.getString(R.string.time_days_ago, diff / 86_400_000)
        else                 -> context.getString(R.string.time_weeks_ago, diff / 604_800_000)
    }
}
