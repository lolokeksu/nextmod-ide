# Основные оптимизации R8
-optimizationpasses 7
-allowaccessmodification
-mergeinterfacesaggressively
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-repackageclasses ''
-keep class com.topjohnwu.superuser.** { *; }
-keepclassmembers class com.topjohnwu.superuser.** { *; }
-dontwarn com.topjohnwu.superuser.**
# NextMod IDE: crash handler — stacktrace должен быть читаемым
-keep class com.magisk.next.CrashHandler { *; }
-keepnames class com.magisk.next.CrashHandler

# NextMod IDE: script linter — object-класс, R8 не должен его инлайнить
-keep class com.magisk.next.viewmodel.ScriptLinter { *; }
-keep enum com.magisk.next.viewmodel.ScriptLinter$Severity { *; }

# Сохранить информацию о строках для читаемых stack trace
-keepattributes SourceFile,LineNumberTable

# Специфичные для проекта правила (если нужны)
# Например, если используется JSONObject через рефлексию – точечно, но здесь нет.
# Для ViewModel AndroidX автоматически подхватываются правила из библиотек.