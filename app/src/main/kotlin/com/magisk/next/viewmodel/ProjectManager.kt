package com.magisk.next.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import com.magisk.next.Logger
import com.magisk.next.R
import com.magisk.next.model.ModuleFile
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectManager(private val data: ModuleData) {

    // [*] saveProject: возвращает Boolean (нет списка для возврата — только пишет данные).
    //     binaryContent сериализуется через Base64 чтобы бинарные файлы не терялись.
    suspend fun saveProject(context: Context, uri: Uri): Boolean = withTimeout(30_000L) {
        try {
            val filesArray = JSONArray().apply {
                data.moduleFiles.forEach { file ->
                    put(JSONObject().apply {
                        put("name", file.name)
                        put("content", file.content)
                        put("permissions", file.permissions)
                        put("type", file.type)
                        // [+] Бинарные файлы сериализуются через Base64
                        if (file.isBinary && file.binaryContent != null) {
                            put("isBinary", true)
                            put("binaryContent", Base64.encodeToString(
                                file.binaryContent, Base64.NO_WRAP
                            ))
                        }
                    })
                }
            }

            val json = JSONObject().apply {
                put("format_version", 2)        // [+] версия формата
                put("moduleId", data.moduleId)
                put("moduleName", data.moduleName)
                put("moduleVersion", data.moduleVersion)
                put("moduleVersionCode", data.moduleVersionCode)
                put("moduleAuthor", data.moduleAuthor)
                put("moduleLink", data.moduleLink)
                put("moduleDescription", data.moduleDescription)
                put("moduleChangelog", data.moduleChangelog)
                put("customizeScript", data.customizeScript)
                put("serviceScript", data.serviceScript)
                put("postFsScript", data.postFsScript)
                put("updateBinaryType", data.updateBinaryType)
                put("updateBinaryCustom", data.updateBinaryCustom)
                put("minMagisk", data.minMagisk)
                put("systemless", data.systemless)
                put("needsystem", data.needsystem)
                put("skipMount", data.skipMount)
                put("recoveryMode", data.recoveryMode)
                put("replaceFiles", data.replaceFiles)
                put("sepolicyRules", data.sepolicyRules)
                put("verifyKey", data.verifyKey)
                put("updateJsonEnabled", data.updateJsonEnabled)
                put("updateJsonUrl", data.updateJsonUrl)
                put("updateZipUrl", data.updateZipUrl)
                put("files", filesArray)
            }

            // [*] Убран двойной ByteArrayOutputStream — пишем напрямую в выходной поток
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zos ->
                    zos.putNextEntry(ZipEntry("project.json"))
                    zos.write(json.toString(2).toByteArray())
                    zos.closeEntry()
                }
            } ?: throw Exception(context.getString(R.string.err_open_file_write))

            Logger.logInfo("Проект сохранён: $uri")
            true
        } catch (e: Exception) {
            Logger.logError("Ошибка сохранения проекта $uri: ${e.message}")
            showToastOnMain(context, context.getString(R.string.toast_project_save_error, e.message ?: ""))
            false
        }
    }

    // [*] loadProject возвращает List<ModuleFile>? вместо Boolean:
    //     - null      → ошибка
    //     - emptyList → проект загружен, файлов нет
    //     - list      → список файлов для применения на main thread в ViewModel
    //     Поля data.* (mutableStateOf) устанавливаются здесь из IO — это thread-safe.
    suspend fun loadProject(context: Context, uri: Uri): List<ModuleFile>? = withTimeout(30_000L) {
        try {
            // [*] Убрано двойное буферирование — читаем поток напрямую
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var jsonString = ""
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "project.json") {
                            jsonString = zis.readBytes().toString(Charsets.UTF_8)
                            break
                        }
                        entry = zis.nextEntry
                    }

                    if (jsonString.isNotEmpty()) {
                        val json = JSONObject(jsonString)

                        data.moduleId          = json.optString("moduleId", "")
                        data.moduleName        = json.optString("moduleName", "")
                        data.moduleVersion     = json.optString("moduleVersion", "")
                        data.moduleVersionCode = json.optString("moduleVersionCode", "")
                        data.moduleAuthor      = json.optString("moduleAuthor", "")
                        data.moduleLink        = json.optString("moduleLink", "")
                        data.moduleDescription = json.optString("moduleDescription", "")
                        data.moduleChangelog   = json.optString("moduleChangelog", "")
                        data.customizeScript   = json.optString("customizeScript", "")
                        data.serviceScript     = json.optString("serviceScript", "")
                        data.postFsScript      = json.optString("postFsScript", "")
                        data.updateBinaryType  = json.optString("updateBinaryType", "symlink")
                        data.updateBinaryCustom = json.optString("updateBinaryCustom", "")
                        data.minMagisk         = json.optString("minMagisk", "20400")
                        data.systemless        = json.optBoolean("systemless", true)
                        data.needsystem        = json.optBoolean("needsystem", false)
                        data.skipMount         = json.optBoolean("skipMount", false)
                        data.recoveryMode      = json.optBoolean("recoveryMode", false)
                        data.replaceFiles      = json.optString("replaceFiles", "")
                        data.sepolicyRules     = json.optString("sepolicyRules", "")
                        data.verifyKey         = json.optString("verifyKey", "")
                        data.updateJsonEnabled = json.optBoolean("updateJsonEnabled", false)
                        data.updateJsonUrl     = json.optString("updateJsonUrl", "")
                        data.updateZipUrl      = json.optString("updateZipUrl", "")

                        val files = mutableListOf<ModuleFile>()
                        val filesArray = json.optJSONArray("files")
                        if (filesArray != null) {
                            for (i in 0 until filesArray.length()) {
                                val fileObj = filesArray.getJSONObject(i)
                                // [+] Восстанавливаем бинарные файлы из Base64
                                val isBinary = fileObj.optBoolean("isBinary", false)
                                val b64 = fileObj.optString("binaryContent", "")
                                val binaryContent = if (isBinary && b64.isNotBlank()) {
                                    Base64.decode(b64, Base64.NO_WRAP)
                                } else null

                                files.add(
                                    ModuleFile(
                                        name          = fileObj.getString("name"),
                                        content       = fileObj.optString("content", ""),
                                        permissions   = fileObj.optString("permissions", "0644"),
                                        type          = fileObj.optString("type", "text"),
                                        isBinary      = isBinary,
                                        binaryContent = binaryContent
                                    )
                                )
                            }
                        }

                        Logger.logInfo("Проект загружен: $uri (файлов: ${files.size})")
                        files
                    } else {
                        // [*] Fallback: ZIP без project.json — пробуем как Magisk-модуль.
                        //     importModule теперь возвращает List<ModuleFile>? — тип совпадает
                        val importedFiles = ModuleImporter(data).importModule(context, uri)
                        if (importedFiles != null) {
                            Logger.logInfo("Модуль импортирован через загрузку проекта: $uri")
                        } else {
                            Logger.logWarning("Не удалось импортировать модуль через загрузку проекта: $uri")
                        }
                        importedFiles
                    }
                }
            } ?: throw Exception(context.getString(R.string.err_open_file))
        } catch (e: Exception) {
            Logger.logError("Ошибка загрузки проекта $uri: ${e.message}")
            showToastOnMain(context, context.getString(R.string.toast_project_load_error, e.message ?: ""))
            null
        }
    }

    // [+] Toast всегда на main thread — на IO-потоке может бросить исключение
    private fun showToastOnMain(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}