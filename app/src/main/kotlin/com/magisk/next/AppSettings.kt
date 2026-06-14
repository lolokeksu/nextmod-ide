package com.magisk.next

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppSettings(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var savePath: String
        get() = prefs.getString("save_path", "MagiskModuleCreator/Project") ?: "MagiskModuleCreator/Project"
        set(value) = prefs.edit { putString("save_path", value) }

    var hiddenExtensions: Set<String>
        get() = prefs.getStringSet("hidden_extensions", emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet("hidden_extensions", value) }

    var notifyAfterExport: Boolean
        get() = prefs.getBoolean("notify_after_export", true)
        set(value) = prefs.edit { putBoolean("notify_after_export", value) }

    var autoSaveOnExit: Boolean
        get() = prefs.getBoolean("auto_save_on_exit", false)
        set(value) = prefs.edit { putBoolean("auto_save_on_exit", value) }

    var showHiddenFiles: Boolean
        get() = prefs.getBoolean("show_hidden_files", false)
        set(value) = prefs.edit { putBoolean("show_hidden_files", value) }

    var logLevel: String
        get() = prefs.getString("log_level", "errors") ?: "errors"
        set(value) = prefs.edit { putString("log_level", value) }

    var themeMode: String
        get() = prefs.getString("theme_mode", "system") ?: "system"
        set(value) = prefs.edit { putString("theme_mode", value) }

    // [+] Root-доступ — пользователь явно включает в настройках
    var rootEnabled: Boolean
        get() = prefs.getBoolean("root_enabled", false)
        set(value) = prefs.edit { putBoolean("root_enabled", value) }

    // [+] Флаг первого запуска — для онбординга
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("is_first_launch", true)
        set(value) = prefs.edit { putBoolean("is_first_launch", value) }

    // [+] Недавние проекты (макс. 5) — хранятся как JSON-массив
    data class RecentProject(val path: String, val name: String, val timestamp: Long)

    fun getRecentProjects(): List<RecentProject> {
        val json = prefs.getString("recent_projects", "[]") ?: "[]"
        return try {
            val array = org.json.JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                val path = obj.optString("path", "")
                if (path.isBlank()) null
                else RecentProject(
                    path      = path,
                    name      = obj.optString("name", "Проект"),
                    timestamp = obj.optLong("timestamp", 0)
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun addRecentProject(path: String, name: String) {
        val current = getRecentProjects().filter { it.path != path }.take(4)
        val updated = listOf(RecentProject(path, name, System.currentTimeMillis())) + current
        val array = org.json.JSONArray()
        updated.forEach { p ->
            array.put(org.json.JSONObject().apply {
                put("path", p.path); put("name", p.name); put("timestamp", p.timestamp)
            })
        }
        prefs.edit { putString("recent_projects", array.toString()) }
    }

    fun removeRecentProject(path: String) {
        val updated = getRecentProjects().filter { it.path != path }
        val array = org.json.JSONArray()
        updated.forEach { p ->
            array.put(org.json.JSONObject().apply {
                put("path", p.path); put("name", p.name); put("timestamp", p.timestamp)
            })
        }
        prefs.edit { putString("recent_projects", array.toString()) }
    }

    // [*] Исправлена сериализация StringSet — JSONObject(map) давал мусор для Set<String>
    fun exportSettings(): String {
        val json = org.json.JSONObject()
        for ((key, value) in prefs.all) {
            when (value) {
                is Set<*> -> json.put(key, org.json.JSONArray(value))
                else      -> json.put(key, value)
            }
        }
        return json.toString()
    }

    // [*] Исправлен импорт StringSet — JSONArray корректно восстанавливается в Set<String>
    // [*] Whitelist известных ключей — импорт не должен писать произвольные/чужие настройки
    private val knownKeys = setOf(
        "save_path", "hidden_extensions", "notify_after_export", "auto_save_on_exit",
        "show_hidden_files", "log_level", "theme_mode", "root_enabled"
    )

    fun importSettings(jsonString: String) {
        try {
            val json = org.json.JSONObject(jsonString)
            prefs.edit {
                for (key in json.keys()) {
                    if (key !in knownKeys) continue  // пропускаем неизвестные ключи
                    when (val value = json.get(key)) {
                        is org.json.JSONArray -> {
                            val set = (0 until value.length())
                                .map { value.getString(it) }
                                .toSet()
                            putStringSet(key, set)
                        }
                        is String  -> putString(key, value)
                        is Boolean -> putBoolean(key, value)
                        is Int     -> putInt(key, value)
                        is Long    -> putLong(key, value)
                        is Float   -> putFloat(key, value)
                        is Double  -> putFloat(key, value.toFloat())
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // [*] suspend + Dispatchers.IO — дисковый I/O убран с main thread
    //     Вызов в SettingsScreen: scope.launch { settings.clearLogs() }
    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        val logFile = java.io.File(
            android.os.Environment.getExternalStorageDirectory(),
            Logger.LOG_FILE
        )
        if (logFile.exists()) logFile.delete()
    }
}