<div align="center">

🌐 [English](README.md) | [Русский](README.ru.md)

<img src="https://github.com/user-attachments/assets/761eddfd-49de-422e-99d0-d5a86664affd" width="120" alt="NextMod IDE"/>

# NextMod IDE

**Полноценная IDE для создания модулей Magisk, KernelSU и APatch — прямо на Android, без ПК.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/платформа-Android%208.0%2B-brightgreen.svg)]()
[![Release](https://img.shields.io/github/v/release/lolokeksu/nextmod-ide)](https://github.com/lolokeksu/nextmod-ide/releases)
[![Telegram Channel](https://img.shields.io/badge/Telegram-канал-2CA5E0?logo=telegram&logoColor=white)](https://t.me/nextmod_ide)
[![Telegram Chat](https://img.shields.io/badge/Telegram-чат-2CA5E0?logo=telegram&logoColor=white)](https://t.me/nextmod_ide_chat)

![Android](https://img.shields.io/badge/Android-IDE-3DDC84?logo=android&logoColor=white)
![Magisk](https://img.shields.io/badge/Magisk-поддерживается-00B0FF)
![KernelSU](https://img.shields.io/badge/KernelSU-поддерживается-7C4DFF)
![APatch](https://img.shields.io/badge/APatch-поддерживается-FF6D00)
![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white)
![Shell](https://img.shields.io/badge/Shell-busybox%20ash%2Fmksh-4CAF50)
![ScriptLinter](https://img.shields.io/badge/ScriptLinter-встроен-19E3D2)

</div>

---

## О приложении

**NextMod IDE** (NXI) — мобильная среда разработки root-модулей. Покрывает полный цикл создания модулей для Magisk / KernelSU / APatch без компьютера: метаданные, скрипты, оверлеи, сборка, валидация и установка — всё на телефоне.

Создано для тех, кто пишет модули в дороге, на планшете или просто без доступа к ПК.

## Скриншоты

| Метаданные | Превью | Менеджер модулей | Браузер |
|------------|--------|------------------|---------|
| <img src="https://github.com/user-attachments/assets/c9ab2438-6b4a-4e4b-8093-4192329bdf7c" width="180"/> | <img src="https://github.com/user-attachments/assets/b26dcbb2-3b47-4b62-9a22-45e4f081fca4" width="180"/> | <img src="https://github.com/user-attachments/assets/a0146e72-83b9-49f5-b629-032a2ec6fe9a" width="180"/> | <img src="https://github.com/user-attachments/assets/fa524708-0b00-45ec-a501-f6c68a9609ba" width="180"/> |

## Возможности

- **Полный сборщик модулей** — `module.prop`, `customize.sh`, `service.sh`, `post-fs-data.sh`, оверлеи `/system`, директории `replace`, `sepolicy.rule`
- **ScriptLinter** — статический анализ на совместимость с busybox `ash` / `mksh`: ловит `local` вне функций, башизмы (`[[ ]]`, массивы), опасные операции (неэкранированный `rm -rf`, запись в разделы)
- **Подсветка синтаксиса** — редактор shell со цветовой разметкой
- **Превью структуры** — дерево ZIP-архива и содержимое `module.prop` до сборки
- **Валидация модуля** — проверки корректности перед экспортом
- **Авто-обновление (updateJson)** — генерирует `update.json` для автообновлений через рут-менеджер
- **Прямая установка** — установка собранного модуля без файлового менеджера (Magisk / KernelSU / APatch)
- **Менеджер модулей** — список установленных модулей, включение / отключение / удаление
- **Файловый браузер** — навигация по файловой системе с root-доступом
- **Шаблоны** — готовые заготовки модулей (деблоатер, hosts-блокировщик, твики ядра)
- **Локализация** — русский и английский
- **Темы** — системная, светлая, тёмная, AMOLED
- **Фирменный дизайн** — авторская палитра, анимированный splash-экран

## Требования

- Android 8.0+ (API 26)
- Root: Magisk, KernelSU или APatch (для установки и менеджера; сборщик работает и без root)

## Установка

1. Скачай APK из раздела [Releases](https://github.com/lolokeksu/nextmod-ide/releases)
2. Установи (разреши установку из неизвестных источников)
3. При запросе предоставь root-доступ для root-функций

## Сборка из исходников

```bash
git clone https://github.com/lolokeksu/nextmod-ide.git
```

Проект собирается на Android (ACSIDE + Termux) или на ПК (Android Studio). Требования: AGP 8.13+, Kotlin 2.0, compileSdk 36, minSdk 26.

> Подпись: создай `keys/keystore.properties` со своими ключами (см. `build.gradle.kts`). Без него собираются только debug-сборки.

## Сообщество

| | |
|---|---|
| 📢 Telegram канал | [@nextmod_ide](https://t.me/nextmod_ide) — релизы и обновления |
| 💬 Telegram чат | [@nextmod_ide_chat](https://t.me/nextmod_ide_chat) — вопросы и фидбек |
| 🐛 Баг-репорты | [GitHub Issues](https://github.com/lolokeksu/nextmod-ide/issues) — используй шаблон |
| 🇷🇺 4PDA | [Тема на форуме](https://4pda.to/forum/index.php?showtopic=1123014) — русскоязычное сообщество |

## Лицензия

Проект распространяется под лицензией **GNU General Public License v3.0** — см. [LICENSE](LICENSE).

Все производные работы (форки) должны оставаться открытыми под той же лицензией. Название «NextMod IDE» и визуальная идентика принадлежат автору; форки обязаны использовать собственное название.

## Автор

**ExchNow** (Lolokeksu)

[![GitHub](https://img.shields.io/badge/GitHub-lolokeksu-181717?logo=github)](https://github.com/lolokeksu)
[![Telegram](https://img.shields.io/badge/Telegram-lolokeksu-2CA5E0?logo=telegram&logoColor=white)](https://t.me/lolokeksu)

---

<div align="center">
Сделано на телефоне — для телефонов.
</div>
