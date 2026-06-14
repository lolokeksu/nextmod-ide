package com.magisk.next.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.magisk.next.Logger
import com.magisk.next.R
import com.magisk.next.model.ModuleFile
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import androidx.core.content.FileProvider

class ModuleBuilder(private val data: ModuleData) {

    // [*] @Volatile — кеш инвалидируется с main thread, читается из Dispatchers.IO
    @Volatile
    private var cachedProp: String? = null

    fun generateModuleProp(): String {
        return cachedProp ?: buildString {
            appendLine("id=${data.moduleId.ifBlank { "template_module" }}")
            appendLine("name=${data.moduleName.ifBlank { "Template Module" }}")
            appendLine("version=${data.moduleVersion.ifBlank { "1.0.0" }}")
            appendLine("versionCode=${data.moduleVersionCode.ifBlank { "1" }}")
            appendLine("author=${data.moduleAuthor.ifBlank { "Developer" }}")
            appendLine("description=${data.moduleDescription.ifBlank { "Description" }}")
            if (data.moduleLink.isNotBlank()) appendLine("link=${data.moduleLink}")
            if (data.moduleChangelog.isNotBlank()) appendLine("changelog=${data.moduleChangelog}")
            if (data.minMagisk.isNotBlank()) appendLine("minMagisk=${data.minMagisk}")
            // [+] Автообновление: Magisk/KSU/APatch читают updateJson и предлагают обновление
            if (data.updateJsonEnabled && data.updateJsonUrl.isNotBlank())
                appendLine("updateJson=${data.updateJsonUrl}")
        }.also { cachedProp = it }
    }

    fun invalidateCache() {
        cachedProp = null
    }

    // [+] Генерация update.json для автообновления модуля.
    //     Файл кладётся на сервер по адресу updateJsonUrl, zipUrl — ссылка на релиз.
    fun generateUpdateJson(): String {
        val version = data.moduleVersion.ifBlank { "1.0.0" }
        // versionCode в JSON должен быть числом — иначе невалидный JSON
        val versionCode = data.moduleVersionCode.toIntOrNull() ?: 1
        val zipUrl = data.updateZipUrl.trim()
        return buildString {
            appendLine("{")
            appendLine("  \"version\": \"$version\",")
            appendLine("  \"versionCode\": $versionCode,")
            appendLine("  \"zipUrl\": \"$zipUrl\",")
            appendLine("  \"changelog\": \"\"")
            append("}")
        }
    }

    // [+] Результат валидации модуля перед экспортом
    data class ValidationResult(
        val errors: List<String>,
        val warnings: List<String>
    ) {
        val isValid: Boolean get() = errors.isEmpty()
    }

    // [+] Проверка модуля перед сборкой — предотвращает экспорт битого ZIP.
    fun validate(context: Context): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Критичные — без них модуль не установится / не имеет смысла
        if (data.moduleId.isBlank()) {
            errors.add(context.getString(R.string.vex_error_id_missing))
        } else if (!data.moduleId.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
            errors.add(context.getString(R.string.vex_error_id_chars))
        }
        if (data.moduleName.isBlank()) errors.add(context.getString(R.string.vex_error_name_missing))
        if (data.moduleVersion.isBlank()) errors.add(context.getString(R.string.vex_error_version_missing))

        val versionCode = data.moduleVersionCode.toIntOrNull()
        if (data.moduleVersionCode.isNotBlank() && versionCode == null) {
            errors.add(context.getString(R.string.vex_error_versioncode))
        }

        // Модуль без содержимого
        val hasFiles   = data.moduleFiles.isNotEmpty()
        val hasScripts = listOf(data.customizeScript, data.serviceScript, data.postFsScript).any { it.isNotBlank() }
        val hasReplace = data.replaceFiles.isNotBlank()
        if (!hasFiles && !hasScripts && !hasReplace) {
            errors.add(context.getString(R.string.vex_error_empty_module))
        }

        // Предупреждения — модуль соберётся, но стоит обратить внимание
        if (data.moduleAuthor.isBlank()) warnings.add(context.getString(R.string.vex_warn_author))
        if (data.moduleDescription.isBlank()) warnings.add(context.getString(R.string.vex_warn_description))
        if (data.minMagisk.isNotBlank() && data.minMagisk.toIntOrNull() == null) {
            warnings.add(context.getString(R.string.vex_warn_minmagisk))
        }
        // Скрипты без shebang
        listOf(
            "customize.sh" to data.customizeScript,
            "service.sh" to data.serviceScript,
            "post-fs-data.sh" to data.postFsScript
        ).forEach { (name, content) ->
            if (content.isNotBlank() && !content.trimStart().startsWith("#!")) {
                warnings.add(context.getString(R.string.vex_warn_no_shebang, name))
            }
        }
        // [+] Статический анализ скриптов (busybox ash / mksh совместимость)
        listOf(
            "customize.sh" to data.customizeScript,
            "service.sh"   to data.serviceScript,
            "post-fs-data.sh" to data.postFsScript
        ).forEach { (name, content) ->
            if (content.isNotBlank()) {
                val issues = ScriptLinter.lint(context, content)
                issues.forEach { issue ->
                    val prefix = when (issue.severity) {
                        ScriptLinter.Severity.CRITICAL -> "⛔"
                        ScriptLinter.Severity.DANGER   -> "⚠️"
                        ScriptLinter.Severity.STYLE    -> "💡"
                    }
                    warnings.add("$prefix $name:${issue.line} — ${issue.message}")
                }
            }
        }

        // Файлы вне стандартных путей оверлея
        data.moduleFiles.forEach { f ->
            val n = f.name.trimStart('/')
            if (!n.startsWith("system/") && !n.startsWith("vendor/") &&
                !n.startsWith("product/") && !n.startsWith("system_ext/") &&
                !n.endsWith(".sh") && !n.contains("/")) {
                warnings.add(context.getString(R.string.vex_warn_file_path, f.name))
            }
        }

        // [+] Проверка автообновления
        if (data.updateJsonEnabled) {
            if (data.updateJsonUrl.isBlank())
                warnings.add(context.getString(R.string.vex_warn_updatejson_url))
            else if (!data.updateJsonUrl.startsWith("http"))
                warnings.add(context.getString(R.string.vex_warn_updatejson_http))
            if (data.updateZipUrl.isBlank())
                warnings.add(context.getString(R.string.vex_warn_updatezip_url))
        }

        return ValidationResult(errors, warnings)
    }

    // [+] Предпросмотр структуры итогового ZIP — список путей с размерами.
    //     Совпадает с реальной логикой buildAndSaveZip.
    data class ZipEntry2(val path: String, val size: Long)

    fun previewZipStructure(): List<ZipEntry2> {
        val entries = mutableListOf<ZipEntry2>()
        val used = mutableSetOf<String>()
        fun add(path: String, size: Long) {
            if (used.add(path)) entries.add(ZipEntry2(path, size))
        }

        add("module.prop", generateModuleProp().toByteArray().size.toLong())
        if (data.customizeScript.isNotBlank()) add("customize.sh", data.customizeScript.toByteArray().size.toLong())
        if (data.serviceScript.isNotBlank())   add("service.sh", data.serviceScript.toByteArray().size.toLong())
        if (data.postFsScript.isNotBlank())     add("post-fs-data.sh", data.postFsScript.toByteArray().size.toLong())

        // [*] Флаги — порядок и набор как в buildAndSaveZip
        if (data.systemless) add("systemless", 0)
        if (data.needsystem) add("needsystem", 0)
        if (data.skipMount) add("skip_mount", 0)
        if (data.recoveryMode) add("recovery_mode", 0)

        data.replaceFiles.lines().filter { it.isNotBlank() }.forEach { line ->
            add("system/${line.trim().trim('/')}/.replace", 0)
        }
        if (data.sepolicyRules.isNotBlank()) add("sepolicy.rule", data.sepolicyRules.toByteArray().size.toLong())
        if (data.verifyKey.isNotBlank()) add("verify", data.verifyKey.toByteArray().size.toLong())

        // [*] update-binary: учитываем custom-вариант как в реальной сборке
        val ub = if (data.updateBinaryType == "custom" && data.updateBinaryCustom.isNotBlank())
                     data.updateBinaryCustom else DEFAULT_UPDATE_BINARY
        add("META-INF/com/google/android/update-binary", ub.toByteArray().size.toLong())
        add("META-INF/com/google/android/updater-script", 8)

        data.moduleFiles.forEach { f ->
            if (!f.name.endsWith("/")) {
                val size = if (f.isBinary && f.binaryContent != null) f.binaryContent!!.size.toLong()
                           else f.content.toByteArray().size.toLong()
                add(f.name, size)
            }
        }
        return entries
    }

    fun getTotalSize(): Long {
        var size = generateModuleProp().toByteArray().size.toLong()
        listOf(data.customizeScript, data.serviceScript, data.postFsScript, data.updateBinaryCustom).forEach {
            if (it.isNotBlank()) size += it.toByteArray().size
        }
        // [*] Для бинарных файлов учитывается реальный binaryContent,
        //     раньше считалось content="[binary]" (8 байт вместо мегабайтов)
        data.moduleFiles.forEach { f ->
            size += if (f.isBinary && f.binaryContent != null) {
                f.binaryContent!!.size.toLong()
            } else {
                f.content.toByteArray().size.toLong()
            }
        }
        return size
    }

    suspend fun buildAndSaveZip(context: Context): Uri? = withTimeout(30_000L) {
        try {
            val tmpFile = File(context.cacheDir, "module_temp_${System.currentTimeMillis()}.zip")
            ZipOutputStream(tmpFile.outputStream()).use { zos ->
                // [*] Уровень 6 вместо 0 — модули с бинарниками заметно меньше,
                //     скорость на телефоне приемлемая
                zos.setLevel(6)
                val usedNames = mutableSetOf<String>()

                fun ZipOutputStream.safePutNextEntry(name: String): Boolean {
                    if (usedNames.add(name)) {
                        putNextEntry(ZipEntry(name))
                        return true
                    }
                    return false
                }

                // [*] КРИТИЧНО: все entry теперь в КОРНЕ ZIP, без префикса moduleId/.
                //     Magisk требует module.prop в корне архива — ZIP с подпапкой
                //     не устанавливается («не выделяйте родительскую папку при сжатии»).

                // module.prop
                zos.safePutNextEntry("module.prop")
                zos.write(generateModuleProp().toByteArray())
                zos.closeEntry()

                addScriptEntrySafe(zos, "customize.sh", data.customizeScript, usedNames)
                addScriptEntrySafe(zos, "service.sh", data.serviceScript, usedNames)
                addScriptEntrySafe(zos, "post-fs-data.sh", data.postFsScript, usedNames)

                // Флаги модуля
                if (data.systemless) {
                    zos.safePutNextEntry("systemless")
                    zos.write(ByteArray(0))
                    zos.closeEntry()
                }
                if (data.needsystem) {
                    zos.safePutNextEntry("needsystem")
                    zos.write(ByteArray(0))
                    zos.closeEntry()
                }
                if (data.skipMount) {
                    zos.safePutNextEntry("skip_mount")
                    zos.write(ByteArray(0))
                    zos.closeEntry()
                }
                if (data.recoveryMode) {
                    zos.safePutNextEntry("recovery_mode")
                    zos.write(ByteArray(0))
                    zos.closeEntry()
                }

                // [*] replace-директории: по семантике Magisk заменяемая директория
                //     помечается ПУСТЫМ файлом .replace внутри неё —
                //     раньше писался текстовый плейсхолдер по самому пути
                val replacePaths = data.replaceFiles.lines().filter { it.isNotBlank() }.map { it.trim().trim('/') }
                for (path in replacePaths) {
                    val entryName = "system/$path/.replace"
                    if (zos.safePutNextEntry(entryName)) {
                        zos.write(ByteArray(0))
                        zos.closeEntry()
                    }
                }

                if (data.sepolicyRules.isNotBlank()) {
                    if (zos.safePutNextEntry("sepolicy.rule")) {
                        zos.write(data.sepolicyRules.toByteArray())
                        zos.closeEntry()
                    }
                }

                if (data.verifyKey.isNotBlank()) {
                    if (zos.safePutNextEntry("verify")) {
                        zos.write(data.verifyKey.toByteArray())
                        zos.closeEntry()
                    }
                }

                // META-INF
                // [*] update-binary — реальный стандартный установщик Magisk,
                //     раньше был пустой файл (прошивка через recovery падала)
                if (zos.safePutNextEntry("META-INF/com/google/android/update-binary")) {
                    val updateBinary = if (data.updateBinaryType == "custom" && data.updateBinaryCustom.isNotBlank()) {
                        data.updateBinaryCustom
                    } else {
                        DEFAULT_UPDATE_BINARY
                    }
                    // [*] Нормализуем CRLF→LF: иначе recovery/ash падает с bad interpreter ^M
                    zos.write(updateBinary.replace("\r\n", "\n").toByteArray())
                    zos.closeEntry()
                }
                if (zos.safePutNextEntry("META-INF/com/google/android/updater-script")) {
                    zos.write("#MAGISK\n".toByteArray())
                    zos.closeEntry()
                }

                for (file in data.moduleFiles) {
                    if (file.name.endsWith("/")) continue
                    val entryName = file.name
                    if (zos.safePutNextEntry(entryName)) {
                        val fileData = if (file.isBinary && file.binaryContent != null) {
                            file.binaryContent!!
                        } else {
                            // [*] CRLF→LF для текстовых файлов (.sh, .conf, .prop и т.п.)
                            file.content.replace("\r\n", "\n").toByteArray()
                        }
                        zos.write(fileData)
                        zos.closeEntry()
                    }
                }
            }

            // Перемещаем временный файл в целевую папку
            // [*] fallback на template_module при пустом moduleId — раньше имя было "-1.0.0.zip"
            val fileName = "${data.moduleId.ifBlank { "template_module" }}-${data.moduleVersion.ifBlank { "1.0.0" }}.zip"
            val appDir = File(Environment.getExternalStorageDirectory(), "MagiskModuleCreator")
            if (!appDir.exists()) appDir.mkdirs()
            val projectDir = File(appDir, "Project")
            if (!projectDir.exists()) projectDir.mkdirs()

            val finalFile = File(projectDir, fileName)
            tmpFile.copyTo(finalFile, overwrite = true)
            tmpFile.delete()

            Logger.logInfo("Модуль экспортирован: ${finalFile.absolutePath}")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", finalFile)
        } catch (e: Exception) {
            Logger.logError("Ошибка экспорта модуля: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun addScriptEntrySafe(
        zos: ZipOutputStream,
        name: String,
        content: String,
        usedNames: MutableSet<String>
    ) {
        if (content.isNotBlank()) {
            val entryName = name
            if (usedNames.add(entryName)) {
                zos.putNextEntry(ZipEntry(entryName))
                // [*] CRLF→LF: shell-скрипты с виндовыми переносами падают с bad interpreter ^M
                zos.write(content.replace("\r\n", "\n").toByteArray())
                zos.closeEntry()
            }
        }
    }

    companion object {
        // [+] Стандартный update-binary из официального шаблона Magisk-модулей:
        //     сорсит util_functions.sh установленного Magisk и вызывает install_module
        private val DEFAULT_UPDATE_BINARY = """#!/sbin/sh

#################
# Initialization
#################

umask 022

# echo before loading util_functions
ui_print() { echo "${'$'}1"; }

require_new_magisk() {
  ui_print "*******************************"
  ui_print " Please install Magisk v20.4+! "
  ui_print "*******************************"
  exit 1
}

#########################
# Load util_functions.sh
#########################

OUTFD=${'$'}2
ZIPFILE=${'$'}3

mount /data 2>/dev/null

[ -f /data/adb/magisk/util_functions.sh ] || require_new_magisk
. /data/adb/magisk/util_functions.sh
[ ${'$'}MAGISK_VER_CODE -lt 20400 ] && require_new_magisk

install_module
exit 0
"""
    }
}