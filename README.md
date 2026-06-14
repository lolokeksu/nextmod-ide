<div align="center">

# NextMod IDE

**A full-fledged IDE for building Magisk, KernelSU and APatch modules — right on your Android device. No PC needed.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-brightgreen.svg)]()
[![Release](https://img.shields.io/github/v/release/lolokeksu/nextmod-ide)](https://github.com/lolokeksu/nextmod-ide/releases)
[![Telegram Channel](https://img.shields.io/badge/Telegram-channel-2CA5E0?logo=telegram&logoColor=white)](https://t.me/nextmod_ide)
[![Telegram Chat](https://img.shields.io/badge/Telegram-chat-2CA5E0?logo=telegram&logoColor=white)](https://t.me/nextmod_ide_chat)

</div>

---

## About

**NextMod IDE** (NXI) is a mobile development environment for root modules. It covers the full module-creation cycle for Magisk / KernelSU / APatch without a computer: metadata, scripts, overlays, building, validation and installation — all on your phone.

Built for people who write modules on the go, on a tablet, or simply without access to a PC.

## Screenshots

<div align="center">

| Metadata | Preview | Module Manager | Browser |
|----------|---------|----------------|---------|
| <img src="https://github.com/user-attachments/assets/c9ab2438-6b4a-4e4b-8093-4192329bdf7c" width="180"/> | <img src="https://github.com/user-attachments/assets/b26dcbb2-3b47-4b62-9a22-45e4f081fca4" width="180"/> | <img src="https://github.com/user-attachments/assets/a0146e72-83b9-49f5-b629-032a2ec6fe9a" width="180"/> | <img src="https://github.com/user-attachments/assets/fa524708-0b00-45ec-a501-f6c68a9609ba" width="180"/> |
</div>

## Features

- **Full module builder** — `module.prop`, `customize.sh`, `service.sh`, `post-fs-data.sh`, `/system` overlays, `replace` directories, `sepolicy.rule`
- **Script linter** — static analysis for busybox `ash` / `mksh` compatibility: catches `local` outside functions, bashisms (`[[ ]]`, arrays), and dangerous operations (unquoted `rm -rf`, writes to partitions)
- **Syntax highlighting** — shell editor with color-coded markup
- **Structure preview** — ZIP archive tree and `module.prop` contents before building
- **Module validation** — correctness checks before export
- **Auto-update (updateJson)** — generates `update.json` for auto-updates via the root manager
- **Direct root install** — install the built module without a file manager (Magisk / KernelSU / APatch)
- **Module manager** — list of installed modules, enable / disable / remove
- **File browser** — root-aware filesystem navigation
- **Templates** — ready-made module starters (debloater, hosts-blocker, kernel tweaks)
- **Localization** — Russian and English
- **Themes** — system, light, dark, AMOLED
- **Branded design** — signature palette, animated splash screen

## Requirements

- Android 8.0+ (API 26)
- Root: Magisk, KernelSU or APatch (for installation and the manager; the builder works without root too)

## Installation

1. Download the APK from the [Releases](https://github.com/lolokeksu/nextmod-ide/releases) section
2. Install it (allow installation from unknown sources)
3. Grant root access when prompted for root features

## Building from source

```bash
git clone https://github.com/lolokeksu/nextmod-ide.git
```

The project builds on Android (ACSIDE + Termux) or on a PC (Android Studio). Requirements: AGP 8.13+, Kotlin 2.0, compileSdk 36, minSdk 26.

> Signing: create `keys/keystore.properties` with your own keys (see `build.gradle.kts`). Without it, only debug builds are produced.

## Community

| | |
|---|---|
| 📢 Telegram channel | [@nextmod_ide](https://t.me/nextmod_ide) — releases and updates |
| 💬 Telegram chat | [@nextmod_ide_chat](https://t.me/nextmod_ide_chat) — questions and feedback |
| 🐛 Bug reports | [GitHub Issues](https://github.com/lolokeksu/nextmod-ide/issues) — please use the template |
| 🇷🇺 4PDA | [Forum thread](https://4pda.to/forum/index.php?showtopic=1123014) — Russian-speaking community |

## License

This project is licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE).

Any derivative works (forks) must remain open under the same license. The name "NextMod IDE" and the visual identity belong to the author; forks must use their own name.

## Author

**ExchNow** (Lolokeksu)

[![GitHub](https://img.shields.io/badge/GitHub-lolokeksu-181717?logo=github)](https://github.com/lolokeksu)
[![Telegram](https://img.shields.io/badge/Telegram-lolokeksu-2CA5E0?logo=telegram&logoColor=white)](https://t.me/lolokeksu)

---

<div align="center">
Made on a phone, for phones.
</div>
