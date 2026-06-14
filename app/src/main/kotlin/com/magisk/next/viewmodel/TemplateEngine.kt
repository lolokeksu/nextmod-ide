package com.magisk.next.viewmodel

import com.magisk.next.model.ModuleFile

class TemplateEngine(private val data: ModuleData) {

    fun applyTemplate(template: ModuleTemplate) {
        when (template) {
            ModuleTemplate.EMPTY          -> resetToTemplate()
            ModuleTemplate.DEBLOATER      -> applyDebloaterTemplate()
            ModuleTemplate.HOSTS_BLOCKER  -> applyHostsBlockerTemplate()
            ModuleTemplate.KERNEL_TWEAKS  -> applyKernelTweaksTemplate()
        }
    }

    // [*] resetToTemplate теперь очищает ВСЕ поля — moduleFiles, флаги, спецполя.
    //     Раньше файлы и флаги оставались от предыдущего состояния.
    fun resetToTemplate() {
        resetAllFields()
        data.moduleId          = "template_module"
        data.moduleName        = "Template Module"
        data.moduleVersion     = "1.0.0"
        data.moduleVersionCode = "1"
        data.moduleAuthor      = "Developer"
        data.moduleDescription = "Шаблонный модуль Magisk"
        data.moduleLink        = "https://github.com/"
        data.moduleChangelog   = "Версия 1.0.0\n- Изначальный релиз шаблона"
        data.customizeScript   = "#!/system/bin/sh\nui_print \"Installing template...\"\nui_print \"Done\""
        data.serviceScript     = "#!/system/bin/sh\nwhile [ \"\$(getprop sys.boot_completed)\" != \"1\" ]; do\n  sleep 1\ndone\n# Your service code here\nexit 0"
        data.postFsScript      = "#!/system/bin/sh\n# This script runs after /data is mounted\nexit 0"
    }

    // [*] Исправлен debloater: rm -rf из customize.sh — прямая запись в /system,
    //     ломает systemless и необратима. Правильный способ — механизм .replace
    //     (пустой файл .replace в директории = Magisk скрывает её через overlay).
    private fun applyDebloaterTemplate() {
        resetAllFields()
        data.moduleId          = "debloater"
        data.moduleName        = "System Debloater"
        data.moduleVersion     = "1.0.0"
        data.moduleVersionCode = "1"
        data.moduleDescription = "Скрывает ненужные системные приложения через Magisk overlay"
        // [+] Правильный способ debloat: поле replaceFiles.
        //     Magisk создаёт .replace-маркер и скрывает директорию, не удаляя из раздела.
        //     Пользователь редактирует список в Advanced → Replace Files.
        data.replaceFiles      = "/system/app/YouTube\n/system/app/Maps\n/system/app/Drive"
        data.customizeScript   = """#!/system/bin/sh
# Debloat via Magisk overlay — systemless, reversible.
# Список директорий для скрытия задаётся в module.prop через .replace-файлы.
# Просто установите модуль — Magisk скроет указанные папки автоматически.
ui_print "Debloat module installed!"
ui_print "Edit the Replace Files list in the app to customize."
"""
    }

    // [*] Исправлен hosts blocker: убран ручной cp -f в /system/etc/hosts.
    //     Magisk автоматически создаёт overlay для $MODPATH/system/etc/hosts —
    //     прямая запись в /system не нужна и нарушает принцип systemless.
    private fun applyHostsBlockerTemplate() {
        resetAllFields()
        data.moduleId          = "hosts_blocker"
        data.moduleName        = "Hosts Blocker"
        data.moduleVersion     = "1.0.0"
        data.moduleVersionCode = "1"
        data.moduleDescription = "Blocks ads and trackers via hosts file overlay"
        // [*] customize.sh не нужен для файлового overlay — Magisk сам монтирует
        data.customizeScript   = """#!/system/bin/sh
# Magisk автоматически монтирует ${'$'}MODPATH/system/etc/hosts поверх системного.
# Дополнительных команд cp/chmod не требуется — systemless overlay работает сам.
ui_print "Custom hosts file installed!"
"""
        data.moduleFiles.add(
            ModuleFile(
                name        = "system/etc/hosts",
                content     = """127.0.0.1 localhost
::1 localhost

# === Blocked domains ===
# Add your domains below, one per line:
# 0.0.0.0 ads.example.com
""",
                permissions = "0644",
                type        = "config"
            )
        )
    }

    // [*] Исправлен Kernel Tweaks: убран хардкод mmcblk0 (eMMC-специфично).
    //     На UFS-устройствах (большинство современных) путь sda/sdc, не mmcblk0.
    //     Добавлено динамическое определение блочного устройства.
    private fun applyKernelTweaksTemplate() {
        resetAllFields()
        data.moduleId          = "kernel_tweaks"
        data.moduleName        = "Kernel Tweaks"
        data.moduleVersion     = "1.0.0"
        data.moduleVersionCode = "1"
        data.moduleDescription = "Applies kernel parameter tweaks at boot"
        data.serviceScript     = """#!/system/bin/sh
# Ждём полной загрузки
while [ "${'$'}(getprop sys.boot_completed)" != "1" ]; do
  sleep 1
done

# === Динамическое определение блочного устройства ===
# Поддерживает eMMC (mmcblk0), UFS (sda/sdc), NVMe (nvme0n1)
BLOCK_DEV=""
for dev in /sys/block/mmcblk0 /sys/block/sda /sys/block/sdc /sys/block/nvme0n1; do
  [ -d "${'$'}dev/queue" ] && BLOCK_DEV="${'$'}dev" && break
done

if [ -n "${'$'}BLOCK_DEV" ]; then
  # I/O scheduler
  # deadline — хорош для случайных чтений (игры, UI)
  # noop     — минимальные накладные расходы на UFS/NVMe
  echo "deadline" > "${'$'}BLOCK_DEV/queue/scheduler" 2>/dev/null

  # Read-ahead buffer (KB): 128–2048
  echo "512" > "${'$'}BLOCK_DEV/queue/read_ahead_kb" 2>/dev/null
fi

# === Управление памятью ===
# Snappiness (0=swap реже, 100=агрессивно): для телефонов рекомендуется 10–40
echo "20" > /proc/sys/vm/swappiness 2>/dev/null

# === Сеть ===
echo "1" > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null

exit 0
"""
        data.customizeScript   = """#!/system/bin/sh
ui_print "Kernel Tweaks module installed!"
ui_print "Tweaks will be applied after next reboot."
"""
    }

    // [+] Общий метод полного сброса всех полей перед применением шаблона.
    //     Гарантирует чистое состояние независимо от предыдущего содержимого.
    private fun resetAllFields() {
        data.moduleId           = ""
        data.moduleName         = ""
        data.moduleVersion      = ""
        data.moduleVersionCode  = ""
        data.moduleAuthor       = ""
        data.moduleLink         = ""
        data.moduleDescription  = ""
        data.moduleChangelog    = ""
        data.customizeScript    = ""
        data.serviceScript      = ""
        data.postFsScript       = ""
        data.updateBinaryType   = "symlink"
        data.updateBinaryCustom = ""
        data.minMagisk          = "20400"
        data.systemless         = true
        data.needsystem         = false
        data.skipMount          = false
        data.recoveryMode       = false
        data.replaceFiles       = ""
        data.sepolicyRules      = ""
        data.verifyKey          = ""
        data.updateJsonEnabled  = false
        data.updateJsonUrl      = ""
        data.updateZipUrl       = ""
        data.moduleFiles.clear()
    }
}