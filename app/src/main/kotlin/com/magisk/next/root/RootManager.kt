package com.magisk.next.root

import com.magisk.next.Logger
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Модели ───────────────────────────────────────────────────────────────────

enum class RootStatus {
    /** Root не запрашивался или недоступен на устройстве */
    UNAVAILABLE,
    /** Пользователь отказал в доступе */
    DENIED,
    /** Root предоставлен */
    GRANTED
}

sealed class RootInstallResult {
    data class Success(val manager: String) : RootInstallResult()
    data class Error(val message: String)   : RootInstallResult()
}

data class InstalledModule(
    val id:                String,
    val name:              String,
    val version:           String,
    val author:            String,
    val description:       String,
    val isEnabled:         Boolean,
    val isMarkedForRemoval: Boolean
)

// ── Singleton ─────────────────────────────────────────────────────────────────

object RootManager {

    @Volatile private var initialized = false

    /**
     * Вызвать один раз при старте приложения (например, в MainActivity.onCreate).
     * Не открывает шелл — только настраивает билдер.
     */
    fun init(debug: Boolean = false) {
        if (initialized) return
        Shell.enableVerboseLogging = debug
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
        initialized = true
    }

    // ── Статус и запрос доступа ───────────────────────────────────────────────

    /**
     * Проверяет текущий статус без нового запроса.
     * Не блокирует UI — вызывать в Dispatchers.IO.
     */
    suspend fun getStatus(): RootStatus = withContext(Dispatchers.IO) {
        try {
            val granted = Shell.isAppGrantedRoot()
            when {
                granted == true  -> RootStatus.GRANTED
                granted == false -> RootStatus.DENIED
                else -> {
                    // null = статус неизвестен, пробуем открыть шелл
                    if (Shell.getShell().isRoot) RootStatus.GRANTED else RootStatus.DENIED
                }
            }
        } catch (e: Exception) {
            Logger.logError("RootManager.getStatus: ${e.message}")
            RootStatus.UNAVAILABLE
        }
    }

    /**
     * Запрашивает root — показывает диалог суперпользователя если нужно.
     * Вызывать из Dispatchers.IO.
     */
    suspend fun requestRoot(): RootStatus = withContext(Dispatchers.IO) {
        try {
            val isRoot = Shell.getShell().isRoot
            if (isRoot) {
                Logger.logInfo("Root доступ предоставлен")
                RootStatus.GRANTED
            } else {
                Logger.logWarning("Root доступ отклонён")
                RootStatus.DENIED
            }
        } catch (e: Exception) {
            Logger.logError("RootManager.requestRoot: ${e.message}")
            RootStatus.UNAVAILABLE
        }
    }

    // ── Информация о менеджере ────────────────────────────────────────────────

    /**
     * Определяет установленный менеджер и его версию.
     * Пробует Magisk → KernelSU → APatch.
     */
    suspend fun getManagerInfo(): String = withContext(Dispatchers.IO) {
        // Magisk
        Shell.cmd("magisk -V").exec().takeIf { it.isSuccess }?.let {
            return@withContext "Magisk ${it.out.firstOrNull() ?: ""}"
        }
        // KernelSU
        Shell.cmd("ksud -V").exec().takeIf { it.isSuccess }?.let {
            return@withContext "KernelSU ${it.out.firstOrNull() ?: ""}"
        }
        // APatch
        Shell.cmd("apd -V").exec().takeIf { it.isSuccess }?.let {
            return@withContext "APatch ${it.out.firstOrNull() ?: ""}"
        }
        "Неизвестный менеджер"
    }

    // ── Установка модуля ──────────────────────────────────────────────────────

    /**
     * Устанавливает модуль по пути к ZIP-файлу напрямую через root-менеджер.
     * Не требует Intent и выбора приложения.
     */
    suspend fun installModule(zipPath: String): RootInstallResult = withContext(Dispatchers.IO) {
        // [*] Пробуем менеджеры по очереди, сохраняя вывод для диагностики.
        //     FLAG_REDIRECT_STDERR (см. init) направляет stderr в out — реальная ошибка.
        val attempts = listOf(
            "Magisk"   to "magisk --install-module \"$zipPath\"",
            "KernelSU" to "ksud module install \"$zipPath\"",
            "APatch"   to "apd module install \"$zipPath\""
        )
        var lastOutput = ""
        for ((manager, cmd) in attempts) {
            val res = Shell.cmd(cmd).exec()
            if (res.isSuccess) {
                Logger.logInfo("Модуль установлен через $manager: $zipPath")
                return@withContext RootInstallResult.Success(manager)
            }
            // Сохраняем вывод только если команда менеджера реально запускалась
            // (не "command not found" — у несуществующего менеджера код 127)
            if (res.code != 127 && res.out.isNotEmpty()) {
                lastOutput = res.out.joinToString("\n").takeLast(500)
            }
        }
        val err = lastOutput.ifBlank { "Ни один root-менеджер не смог установить модуль" }
        Logger.logError("Ошибка установки модуля: $err")
        RootInstallResult.Error(err)
    }

    // ── Список установленных модулей ──────────────────────────────────────────

    suspend fun getInstalledModules(): List<InstalledModule> = withContext(Dispatchers.IO) {
        // [*] Один проход shell вместо 3 вызовов на модуль: обходим все модули,
        //     для каждого печатаем module.prop + флаги между маркерами ===MOD:id===.
        //     Для 15 модулей: 1 root-вызов вместо ~45 — кратное ускорение.
        val script = """
            for d in /data/adb/modules/*/; do
                [ -d "${'$'}d" ] || continue
                id=${'$'}(basename "${'$'}d")
                echo "===MOD:${'$'}id==="
                [ -f "${'$'}d/disable" ] && echo "@@DISABLED" || echo "@@ENABLED"
                [ -f "${'$'}d/remove" ] && echo "@@REMOVE"
                [ -f "${'$'}d/module.prop" ] && cat "${'$'}d/module.prop"
            done
        """.trimIndent()

        val result = Shell.cmd(script).exec()
        if (!result.isSuccess) return@withContext emptyList()

        val modules = mutableListOf<InstalledModule>()
        var id = ""
        var enabled = true
        var remove = false
        val props = mutableMapOf<String, String>()

        fun flush() {
            if (id.isNotBlank()) {
                modules.add(
                    InstalledModule(
                        id = id,
                        name = props["name"] ?: id,
                        version = props["version"] ?: "",
                        author = props["author"] ?: "",
                        description = props["description"] ?: "",
                        isEnabled = enabled,
                        isMarkedForRemoval = remove
                    )
                )
            }
        }

        for (line in result.out) {
            when {
                line.startsWith("===MOD:") -> {
                    flush()
                    id = line.removePrefix("===MOD:").removeSuffix("===")
                    enabled = true; remove = false; props.clear()
                }
                line == "@@DISABLED" -> enabled = false
                line == "@@ENABLED"  -> enabled = true
                line == "@@REMOVE"   -> remove = true
                line.contains("=") && !line.startsWith("@@") -> {
                    val idx = line.indexOf('=')
                    props[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
                }
            }
        }
        flush()
        modules
    }

    // ── Управление модулями ───────────────────────────────────────────────────

    suspend fun setModuleEnabled(moduleId: String, enabled: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val flagFile = "/data/adb/modules/$moduleId/disable"
            val cmd = if (enabled) "rm -f $flagFile" else "touch $flagFile"
            Shell.cmd(cmd).exec().isSuccess.also {
                Logger.logInfo("Модуль $moduleId: ${if (enabled) "включён" else "отключён"}")
            }
        }

    suspend fun removeModule(moduleId: String): Boolean = withContext(Dispatchers.IO) {
        Shell.cmd("touch /data/adb/modules/$moduleId/remove").exec().isSuccess.also {
            Logger.logInfo("Модуль $moduleId помечен для удаления")
        }
    }

    // ── Утилита ───────────────────────────────────────────────────────────────

    /** Выполнить произвольную root-команду. */
    suspend fun exec(command: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val result = Shell.cmd(command).exec()
        Pair(result.isSuccess, result.out.joinToString("\n"))
    }
}
