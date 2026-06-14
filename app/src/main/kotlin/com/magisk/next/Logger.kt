package com.magisk.next

import android.os.Environment
import java.io.File

object Logger {
    const val LOG_FILE = "MagiskModuleCreator/module_creator.log"
    private const val MAX_LOG_SIZE = 256 * 1024L  // 256 КБ

    private var currentLevel: String = "all"

    // [*] DateTimeFormatter вместо SimpleDateFormat — thread-safe, создаётся один раз
    //     (API 26+ = minSdk проекта, совместимо)
    private val formatter = java.time.format.DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(java.time.ZoneId.systemDefault())

    // [*] Флаг создания директории — dir.exists()/mkdirs() вызываются только один раз
    @Volatile private var dirEnsured = false

    fun setLevel(level: String) {
        currentLevel = level
    }

    fun log(message: String, level: String = "info") {
        val shouldLog = when (currentLevel) {
            "errors" -> level == "error"
            "errors+warnings" -> level == "error" || level == "warning"
            "all" -> true
            else -> true
        }
        if (!shouldLog) return

        try {
            // [*] mkdirs() только при первом вызове
            if (!dirEnsured) {
                val dir = File(Environment.getExternalStorageDirectory(), "MagiskModuleCreator")
                if (!dir.exists()) dir.mkdirs()
                dirEnsured = true
            }
            val logFile = File(Environment.getExternalStorageDirectory(), LOG_FILE)

            // Ротация при превышении размера
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                val oldFile = File(logFile.absolutePath + ".old")
                oldFile.delete()
                logFile.renameTo(oldFile)
            }

            // [*] formatter — thread-safe, создан один раз
            val timestamp = formatter.format(java.time.Instant.now())
            logFile.appendText("[$timestamp] [$level] $message\n")
        } catch (_: Exception) {}
    }

    fun logError(message: String) = log(message, "error")
    fun logWarning(message: String) = log(message, "warning")
    fun logInfo(message: String) = log(message, "info")
}