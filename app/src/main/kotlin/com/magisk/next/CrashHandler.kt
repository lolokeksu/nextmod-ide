package com.magisk.next

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Глобальный обработчик необработанных исключений.
 *
 * Подключение (MainActivity.onCreate, ДО setContent):
 *     CrashHandler.install(this)
 *
 * Проверка при следующем запуске:
 *     CrashHandler.getPendingCrashLog(this)  // null, если крашей не было
 *     CrashHandler.markCrashSeen(this)       // после показа пользователю
 */
object CrashHandler {

    private const val CRASH_FILE = "crash.log"
    private const val EXTERNAL_DIR = "MagiskModuleCreator"
    private const val PREFS = "crash_handler"
    private const val KEY_PENDING = "pending_crash"

    @Volatile
    private var installed = false

    private val formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    fun install(context: Context) {
        if (installed) return
        installed = true

        // applicationContext — чтобы не держать ссылку на Activity
        val appContext = context.applicationContext
        val systemHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // [!] Обработчик не имеет права упасть сам — каждая операция изолирована
            runCatching { writeCrashReport(appContext, thread, throwable) }

            // [!] Обязательно отдаём краш системному обработчику:
            //     стандартный диалог + корректное завершение процесса.
            if (systemHandler != null) {
                systemHandler.uncaughtException(thread, throwable)
            } else {
                Runtime.getRuntime().exit(2)
            }
        }
    }

    /** Возвращает текст последнего краша, если он ещё не показан пользователю. */
    fun getPendingCrashLog(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_PENDING, false)) return null
        return runCatching {
            File(context.filesDir, CRASH_FILE).takeIf { it.exists() }?.readText()
        }.getOrNull()
    }

    /** Пометить краш просмотренным — диалог больше не появится. */
    fun markCrashSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PENDING, false).apply()
    }

    // ------------------------------------------------------------------

    private fun writeCrashReport(context: Context, thread: Thread, throwable: Throwable) {
        val report = buildReport(context, thread, throwable)

        // 1) Внутреннее хранилище — доступно всегда, разрешения не нужны
        runCatching {
            File(context.filesDir, CRASH_FILE).writeText(report)
        }

        // 2) Внешнее хранилище — best effort: чтобы пользователь мог
        //    достать файл руками и приложить к багрепорту
        runCatching {
            val dir = File(Environment.getExternalStorageDirectory(), EXTERNAL_DIR)
            if (!dir.exists()) dir.mkdirs()
            File(dir, CRASH_FILE).writeText(report)
        }

        // 3) Флаг для следующего запуска.
        //    commit(), а не apply() — процесс умрёт сразу после выхода отсюда,
        //    асинхронная запись не успеет завершиться.
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_PENDING, true).commit()
        }
    }

    private fun buildReport(context: Context, thread: Thread, throwable: Throwable): String {
        val stacktrace = StringWriter().also { sw ->
            throwable.printStackTrace(PrintWriter(sw))
        }.toString()

        val version = runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pi.versionName} (${if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()})"
        }.getOrDefault("unknown")

        return buildString {
            appendLine("===== CRASH REPORT =====")
            appendLine("Время:      ${formatter.format(Instant.now())}")
            appendLine("Приложение: ${context.packageName} $version")
            appendLine("Устройство: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android:    ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Поток:      ${thread.name}")
            appendLine("========================")
            appendLine(stacktrace)
        }
    }
}
