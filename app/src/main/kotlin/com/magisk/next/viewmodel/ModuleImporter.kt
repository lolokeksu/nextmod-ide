package com.magisk.next.viewmodel

import android.content.Context
import android.net.Uri
import com.magisk.next.Logger
import com.magisk.next.model.ModuleFile
import java.util.zip.ZipInputStream

class ModuleImporter(private val data: ModuleData) {

    // [*] Возвращает List<ModuleFile>? вместо Boolean:
    //     - null  → ошибка импорта
    //     - list  → успех; мутация data.moduleFiles выполняется в ViewModel на main thread
    //     Поля data.* (mutableStateOf) безопасно устанавливаются здесь из IO-потока.
    fun importModule(context: Context, uri: Uri): List<ModuleFile>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->

                    // [*] Собираем все записи сразу — надёжное определение префикса
                    //     без зависимости от порядка записей в архиве
                    val rawEntries = mutableMapOf<String, ByteArray>()
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            // [*] Защита от OOM: пропускаем записи > 10 МБ
                            val size = entry.size
                            if (size < 0 || size <= 10 * 1024 * 1024) {
                                rawEntries[entry.name] = zis.readBytes()
                            } else {
                                Logger.logWarning("Пропущена запись ${entry.name}: размер $size байт превышает лимит")
                            }
                        }
                        entry = zis.nextEntry
                    }

                    // [*] Определяем префикс (moduleId/) один раз по позиции module.prop
                    val prefix = when {
                        rawEntries.containsKey("module.prop") -> ""
                        else -> rawEntries.keys
                            .firstOrNull { it.endsWith("/module.prop") }
                            ?.substringBeforeLast("/module.prop")
                            ?.let { "$it/" } ?: ""
                    }

                    val files = mutableListOf<ModuleFile>()
                    val replaceFiles = mutableListOf<String>()

                    for ((name, bytes) in rawEntries) {
                        // Пропускаем META-INF и update-binary
                        if (name.contains("META-INF")) continue

                        // Убираем префикс, получаем путь относительно корня модуля
                        val relativePath = if (prefix.isNotEmpty() && name.startsWith(prefix)) {
                            name.removePrefix(prefix)
                        } else {
                            name
                        }

                        if (relativePath.isBlank()) continue

                        val fileName = relativePath.substringAfterLast("/")

                        when {
                            // module.prop
                            relativePath == "module.prop" -> {
                                val props = parseModuleProp(bytes.toString(Charsets.UTF_8))
                                data.moduleId          = props["id"] ?: ""
                                data.moduleName        = props["name"] ?: ""
                                data.moduleVersion     = props["version"] ?: ""
                                data.moduleVersionCode = props["versionCode"] ?: ""
                                data.moduleAuthor      = props["author"] ?: ""
                                data.moduleLink        = props["link"] ?: ""
                                data.moduleDescription = props["description"] ?: ""
                                data.moduleChangelog   = props["changelog"] ?: ""
                                data.minMagisk         = props["minMagisk"] ?: ""
                                // [+] Автообновление: восстанавливаем updateJson при импорте
                                val updJson = props["updateJson"] ?: ""
                                if (updJson.isNotBlank()) {
                                    data.updateJsonEnabled = true
                                    data.updateJsonUrl = updJson
                                }
                            }

                            // Скрипты
                            relativePath == "customize.sh"     -> data.customizeScript = bytes.toString(Charsets.UTF_8)
                            relativePath == "service.sh"       -> data.serviceScript    = bytes.toString(Charsets.UTF_8)
                            relativePath == "post-fs-data.sh"  -> data.postFsScript     = bytes.toString(Charsets.UTF_8)

                            // [+] Восстанавливаем поле sepolicyRules из файла модуля
                            relativePath == "sepolicy.rule" || relativePath == "sepolicy.rules" -> {
                                data.sepolicyRules = bytes.toString(Charsets.UTF_8)
                            }

                            // [+] Восстанавливаем флаги модуля
                            relativePath == "skip_mount"    -> data.skipMount    = true
                            relativePath == "systemless"    -> data.systemless   = true
                            relativePath == "needsystem"    -> data.needsystem   = true
                            relativePath == "recovery_mode" -> data.recoveryMode = true

                            // [+] Восстанавливаем replaceFiles: .replace = маркер Magisk для замены директории
                            fileName == ".replace" -> {
                                val dir = relativePath
                                    .removePrefix("system/")
                                    .removeSuffix("/.replace")
                                if (dir.isNotBlank()) replaceFiles.add(dir)
                            }

                            // Прочие файлы — с детектом бинарного содержимого
                            else -> {
                                val isBinary = isBinaryContent(bytes)
                                files.add(
                                    ModuleFile(
                                        name        = relativePath,
                                        content     = if (isBinary) "[binary]" else bytes.toString(Charsets.UTF_8),
                                        permissions = if (fileName.endsWith(".sh")) "0755" else "0644",
                                        type        = when {
                                            fileName.endsWith(".sh") -> "script"
                                            isBinary -> "binary"
                                            else -> "text"
                                        },
                                        isBinary      = isBinary,
                                        binaryContent = if (isBinary) bytes else null
                                    )
                                )
                            }
                        }
                    }

                    // Восстанавливаем поле replaceFiles
                    if (replaceFiles.isNotEmpty()) {
                        data.replaceFiles = replaceFiles.joinToString("\n")
                    }

                    Logger.logInfo("Модуль импортирован: $uri (файлов: ${files.size})")
                    files  // возвращаем список — мутация moduleFiles на main thread в ViewModel
                }
            } // [*] если openInputStream вернул null — возвращаем null (ошибка)
        } catch (e: Exception) {
            Logger.logError("Ошибка импорта модуля $uri: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // [*] Детект бинарного содержимого: нулевой байт или невалидный UTF-8
    private fun isBinaryContent(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val testSize = minOf(bytes.size, 512)
        for (i in 0 until testSize) {
            if (bytes[i] == 0.toByte()) return true
        }
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes, 0, testSize))
            false
        } catch (e: Exception) {
            true
        }
    }

    private fun parseModuleProp(content: String): Map<String, String> {
        val props = mutableMapOf<String, String>()
        content.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.contains("=") && !trimmed.startsWith("#")) {
                val key   = trimmed.substringBefore("=").trim()
                val value = trimmed.substringAfter("=").trim()
                props[key] = value
            }
        }
        return props
    }
}