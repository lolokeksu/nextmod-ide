package com.magisk.next.viewmodel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magisk.next.Logger
import com.magisk.next.model.ModuleFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.CodingErrorAction

class ModuleViewModel : ViewModel() {

    internal val data = ModuleData()

    private val builder = ModuleBuilder(data)

    // Делегаты
    var moduleId: String
        get() = data.moduleId
        set(value) { data.moduleId = value; builder.invalidateCache() }
    var moduleName: String
        get() = data.moduleName
        set(value) { data.moduleName = value; builder.invalidateCache() }
    var moduleVersion: String
        get() = data.moduleVersion
        set(value) { data.moduleVersion = value; builder.invalidateCache() }
    var moduleVersionCode: String
        get() = data.moduleVersionCode
        set(value) { data.moduleVersionCode = value; builder.invalidateCache() }
    var moduleAuthor: String
        get() = data.moduleAuthor
        set(value) { data.moduleAuthor = value; builder.invalidateCache() }
    var moduleLink: String
        get() = data.moduleLink
        set(value) { data.moduleLink = value; builder.invalidateCache() }
    var moduleDescription: String
        get() = data.moduleDescription
        set(value) { data.moduleDescription = value; builder.invalidateCache() }
    var moduleChangelog: String
        get() = data.moduleChangelog
        set(value) { data.moduleChangelog = value; builder.invalidateCache() }
    var customizeScript: String
        get() = data.customizeScript
        set(value) { data.customizeScript = value; builder.invalidateCache() }
    var serviceScript: String
        get() = data.serviceScript
        set(value) { data.serviceScript = value; builder.invalidateCache() }
    var postFsScript: String
        get() = data.postFsScript
        set(value) { data.postFsScript = value; builder.invalidateCache() }
    var updateBinaryType: String
        get() = data.updateBinaryType
        set(value) { data.updateBinaryType = value; builder.invalidateCache() }
    var updateBinaryCustom: String
        get() = data.updateBinaryCustom
        set(value) { data.updateBinaryCustom = value; builder.invalidateCache() }
    var minMagisk: String
        get() = data.minMagisk
        set(value) { data.minMagisk = value; builder.invalidateCache() }
    var systemless: Boolean
        get() = data.systemless
        set(value) { data.systemless = value; builder.invalidateCache() }
    var needsystem: Boolean
        get() = data.needsystem
        set(value) { data.needsystem = value; builder.invalidateCache() }
    var skipMount: Boolean
        get() = data.skipMount
        set(value) { data.skipMount = value; builder.invalidateCache() }
    var recoveryMode: Boolean
        get() = data.recoveryMode
        set(value) { data.recoveryMode = value; builder.invalidateCache() }
    var replaceFiles: String
        get() = data.replaceFiles
        set(value) { data.replaceFiles = value; builder.invalidateCache() }
    var sepolicyRules: String
        get() = data.sepolicyRules
        set(value) { data.sepolicyRules = value; builder.invalidateCache() }
    var verifyKey: String
        get() = data.verifyKey
        set(value) { data.verifyKey = value; builder.invalidateCache() }
    var updateJsonEnabled: Boolean
        get() = data.updateJsonEnabled
        set(value) { data.updateJsonEnabled = value; builder.invalidateCache() }
    var updateJsonUrl: String
        get() = data.updateJsonUrl
        set(value) { data.updateJsonUrl = value; builder.invalidateCache() }
    var updateZipUrl: String
        get() = data.updateZipUrl
        set(value) { data.updateZipUrl = value }

    val moduleFiles = data.moduleFiles

    private val projectManager = ProjectManager(data)
    private val importer = ModuleImporter(data)
    private val templateEngine = TemplateEngine(data)

    private val _validationResults = MutableStateFlow<List<ValidationItem>>(emptyList())
    val validationResults: StateFlow<List<ValidationItem>> = _validationResults

    fun validate(context: Context): List<ValidationItem> {
        val items = ModuleValidator(
            moduleId, moduleName, moduleVersion, moduleDescription,
            customizeScript, moduleFiles.size
        ).validate(context)
        _validationResults.value = items
        return items
    }

    // [*] Regex кешированы в companion — не создаются при каждом вызове
    fun isValid(): Boolean {
        return moduleId.isNotBlank() && moduleId.matches(MODULE_ID_REGEX) &&
                moduleName.isNotBlank() &&
                moduleVersion.isNotBlank() && moduleVersion.matches(MODULE_VERSION_REGEX)
    }

    fun generateModuleProp(): String = builder.generateModuleProp()
    fun generateUpdateJson(): String = builder.generateUpdateJson()
    fun getTotalSize(): Long = builder.getTotalSize()

    // [+] Валидация перед экспортом
    fun validateForExport(context: Context): ModuleBuilder.ValidationResult = builder.validate(context)

    // [+] Предпросмотр структуры ZIP
    fun previewZipStructure(): List<ModuleBuilder.ZipEntry2> = builder.previewZipStructure()

    fun buildAndSaveZip(context: Context, onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                builder.buildAndSaveZip(context)
            }
            onResult(uri)
        }
    }

    fun saveProject(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                projectManager.saveProject(context, uri)
            }
            onResult(result)
        }
    }

    fun loadProject(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
    viewModelScope.launch {
        // [+] Сбрасываем текущее состояние перед загрузкой нового проекта
        resetAll()
        val loadedFiles = withContext(Dispatchers.IO) {
            projectManager.loadProject(context, uri)
        }
        if (loadedFiles != null) {
            data.moduleFiles.clear()
            data.moduleFiles.addAll(loadedFiles)
            builder.invalidateCache()
            onResult(true)
        } else {
            onResult(false)
        }
    }
}

    fun importModule(context: Context, uri: Uri) {
        viewModelScope.launch {
            // [*] importModule возвращает List<ModuleFile>? — мутация SnapshotStateList
            //     (mutableStateListOf) выполняется здесь на main thread, а не в IO-потоке
            // [+] Сброс перед импортом (main thread — resetAll трогает SnapshotStateList)
            resetAll()
            val importedFiles = withContext(Dispatchers.IO) {
                importer.importModule(context, uri)
            }
            if (importedFiles != null) {
                data.moduleFiles.clear()
                data.moduleFiles.addAll(importedFiles)
                builder.invalidateCache()
            }
        }
    }

    fun applyTemplate(template: ModuleTemplate) {
        templateEngine.applyTemplate(template)
        builder.invalidateCache()
    }

    fun resetToTemplate() {
        templateEngine.resetToTemplate()
        builder.invalidateCache()
    }

    fun resetAll() {
        data.moduleId = ""
        data.moduleName = ""
        data.moduleVersion = ""
        data.moduleVersionCode = ""
        data.moduleAuthor = ""
        data.moduleLink = ""
        data.moduleDescription = ""
        data.moduleChangelog = ""
        data.customizeScript = ""
        data.serviceScript = ""
        data.postFsScript = ""
        data.updateBinaryType = "symlink"
        data.updateBinaryCustom = ""
        data.minMagisk = "20400"
        data.systemless = true
        data.needsystem = false
        data.skipMount = false
        data.updateJsonEnabled = false
        data.updateJsonUrl = ""
        data.updateZipUrl = ""
        data.recoveryMode = false
        data.replaceFiles = ""
        data.sepolicyRules = ""
        data.verifyKey = ""
        data.moduleFiles.clear()
        builder.invalidateCache()
    }

    fun fillDefaultCustomize() {
        val modId = moduleId.ifBlank { "my_module" }
        customizeScript = """#!/system/bin/sh
# This is the customize.sh script for $modId

ui_print "********************************"
ui_print "  $modId"
ui_print "  Installing..."
ui_print "********************************"

set_perm_recursive ${'$'}MODPATH 0 0 0755 0644

# cp -f ${'$'}MODPATH/system/bin/my_tool /system/bin/my_tool
# set_perm /system/bin/my_tool 0 0 0755

ui_print "Installation complete!"
"""
    }

    fun fillDefaultService() {
        serviceScript = """#!/system/bin/sh
# This is the service.sh script

while [ "\$(getprop sys.boot_completed)" != "1" ]; do
  sleep 1
done

# /system/bin/my_daemon &

exit 0
"""
    }

    fun fillDefaultPostFs() {
        postFsScript = """#!/system/bin/sh
# This script runs after /data is mounted

# mount -o rw,remount /system
# echo "127.0.0.1 example.com" >> /system/etc/hosts

exit 0
"""
    }

    // [*] Добавлена ветка "postfs" — раньше сниппеты для post-fs-data.sh молча игнорировались
    fun insertSnippet(snippet: String, target: String) {
        when (target) {
            "customize" -> customizeScript += snippet
            "service" -> serviceScript += snippet
            "postfs" -> postFsScript += snippet
        }
    }

    // [*] I/O вынесен в Dispatchers.IO — чтение файла больше не блокирует main thread.
    //     Общая логика addFileFromUri/addFileFromFile слита в addFileInternal.
    fun addFileFromUri(context: Context, uri: Uri, originalName: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    // [+] Запрашиваем MIME-тип — используется для более точного детекта
                    val mimeType = context.contentResolver.getType(uri)
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) Pair(bytes, mimeType) else null
                } catch (e: Exception) {
                    Logger.logError("Ошибка чтения файла $originalName: ${e.message}")
                    null
                }
            }
            if (result != null) {
                val (bytes, mimeType) = result
                addFileInternal(bytes, originalName.substringAfterLast("/"), mimeType)
            } else {
                Toast.makeText(context, "Ошибка чтения файла", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // [*] I/O вынесен в Dispatchers.IO
    fun addFileFromFile(context: Context, file: File, onAdded: (() -> Unit)? = null) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                try {
                    file.readBytes()
                } catch (e: Exception) {
                    Logger.logError("Ошибка чтения файла ${file.name}: ${e.message}")
                    null
                }
            }
            if (bytes != null) {
                addFileInternal(bytes, file.name, mimeType = null)
                onAdded?.invoke()
            } else {
                Toast.makeText(context, "Ошибка чтения файла", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // [+] Общая логика добавления файла — устраняет дублирование между addFileFromUri и addFileFromFile.
    //     Вызывается на main thread (mutableStateList безопасно мутировать только с main).
    private fun addFileInternal(bytes: ByteArray, displayName: String, mimeType: String? = null) {
        val (type, permissions) = detectFileTypeAndPermissions(displayName, bytes, mimeType)
        val isBinary = isBinaryContent(bytes)
        if (isBinary) {
            moduleFiles.add(
                ModuleFile(
                    name          = displayName,
                    content       = "[binary]",
                    permissions   = permissions,
                    type          = "binary",
                    isBinary      = true,
                    binaryContent = bytes
                )
            )
        } else {
            moduleFiles.add(
                ModuleFile(
                    name        = displayName,
                    content     = String(bytes, Charsets.UTF_8),
                    permissions = permissions,
                    type        = type
                )
            )
        }
        Logger.logInfo("Файл добавлен: $displayName (тип: $type, права: $permissions)")
    }

    // [+] Расширенный детект типа и прав доступа.
    //     Приоритет: MIME → shebang → расширение → путь → content.
    private fun detectFileTypeAndPermissions(
        name: String,
        bytes: ByteArray,
        mimeType: String? = null
    ): Pair<String, String> {
        val lower = name.lowercase()
        val ext = lower.substringAfterLast(".", "")

        // 1. MIME-тип (наиболее точный источник при добавлении через пикер)
        if (mimeType != null) {
            when {
                mimeType.startsWith("image/") ||
                mimeType.startsWith("audio/") ||
                mimeType.startsWith("video/") ||
                mimeType == "application/octet-stream" && EXT_BINARY.contains(ext) ->
                    return "binary" to "0644"
                mimeType in listOf("application/x-sh", "text/x-sh", "application/x-shellscript") ->
                    return "script" to "0755"
                mimeType in listOf("application/json", "text/xml", "application/xml") ->
                    return "config" to "0644"
            }
        }

        // 2. Бинарное содержимое по байтам
        if (isBinaryContent(bytes)) return "binary" to "0755"

        // 3. Shebang в первой строке — точный признак скрипта
        val firstLine = bytes.take(128).toByteArray().toString(Charsets.UTF_8)
            .lines().firstOrNull()?.trim() ?: ""
        if (firstLine.startsWith("#!")) return "script" to "0755"

        // 4. Детект по расширению
        if (EXT_SCRIPTS.contains(ext)) return "script" to "0755"
        if (EXT_CONFIG.contains(ext))  return "config" to "0644"
        if (EXT_BINARY.contains(ext))  return "binary" to "0755"

        // 5. Детект по пути — исполняемые директории
        if (PATH_EXECUTABLE.any { lower.contains(it) }) return "text" to "0755"

        return "text" to "0644"
    }

    // [*] I/O вынесен в Dispatchers.IO
    fun deleteFileFromDisk(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (file.delete()) {
                    Logger.logInfo("Файл удалён с диска: ${file.absolutePath}")
                } else {
                    Logger.logError("Не удалось удалить файл: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                Logger.logError("Ошибка удаления файла ${file.absolutePath}: ${e.message}")
            }
        }
    }

    fun saveModuleProp(context: Context): Uri? {
        return try {
            val prop = builder.generateModuleProp()
            val bytes = prop.toByteArray()
            val fileName = "module.prop"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(bytes) } }
                uri
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, fileName)
                file.writeBytes(bytes)
                // [*] FileProvider вместо Uri.fromFile — file:// URI запрещён к передаче с API 24+
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка сохранения module.prop: ${e.message}", Toast.LENGTH_SHORT).show()
            Logger.logError("Ошибка сохранения module.prop: ${e.message}")
            null
        }
    }

    // [*] Исправлен детект бинарного содержимого: String(bytes, UTF_8) никогда
    //     не бросает исключение (невалидные байты заменяются на U+FFFD),
    //     поэтому старая ветка try/catch всегда возвращала false.
    //     CharsetDecoder со строгим REPORT-режимом реально детектит невалидный UTF-8.
    private fun isBinaryContent(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val testSize = minOf(bytes.size, 512)
        for (i in 0 until testSize) {
            if (bytes[i] == 0.toByte()) return true
        }
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes, 0, testSize))
            false
        } catch (e: Exception) {
            true
        }
    }

    companion object {
        // [*] Regex создаются один раз
        private val MODULE_ID_REGEX = Regex("^[a-zA-Z0-9_]+$")
        private val MODULE_VERSION_REGEX = Regex("^[a-zA-Z0-9.\\-_]+$")

        // [+] Множества для детекта типа файла — создаются один раз при загрузке класса
        val EXT_SCRIPTS = setOf(
            "sh", "bash", "zsh", "ash", "ksh", "fish",
            "py", "rb", "pl", "lua", "awk", "sed"
        )
        val EXT_CONFIG = setOf(
            "prop", "properties", "conf", "cfg", "ini",
            "toml", "yaml", "yml", "json", "xml",
            "env", "rc", "profile", "bashrc", "zshrc",
            "gradle", "mk", "bp"
        )
        val EXT_BINARY = setOf(
            "so", "bin", "dex", "odex", "vdex",
            "apk", "jar", "zip", "7z", "gz",
            "png", "jpg", "jpeg", "webp", "gif", "svg",
            "ttf", "otf", "db", "sqlite"
        )
        val PATH_EXECUTABLE = listOf(
            "system/bin/", "system/xbin/",
            "system/sbin/", "vendor/bin/"
        )

        val SNIPPETS = mapOf(
            "customize" to listOf(
                "mount" to "\n# Монтирование раздела\nmount -o rw,remount /system",
                "copy_dir" to "\n# Копирование директории\ncopy_dir \$MODPATH/system \$SYSTEM",
                "chmod" to "\n# Установка прав доступа\nset_perm_recursive \$MODPATH/system 0 0 0755 0644",
                "setprop" to "\n# Установка свойства\nsetprop persist.my_module.enabled true",
                "ui_print" to "\n# Вывод в консоль Magisk\nui_print \"Installing my module...\""
            ),
            "service" to listOf(
                "while loop" to "\nwhile [ \"\$(getprop sys.boot_completed)\" != \"1\" ]; do\n  sleep 1\ndone",
                "sleep" to "\nsleep 5",
                "am" to "\nam start -n com.example/.MainActivity",
                "settings" to "\nsettings put global my_setting 1",
                "log" to "\necho \"Service started\" >> /data/local/tmp/log.txt"
            )
        )
    }
}