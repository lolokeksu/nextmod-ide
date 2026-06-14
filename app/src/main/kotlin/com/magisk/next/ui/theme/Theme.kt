package com.magisk.next.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.magisk.next.AppSettings

// ── Статичные цвета (не меняются с темой) ───────────────────────────────────
// [*] Брендовая палитра NextMod IDE — в тон иконке и сплешу
val Accent    = Color(0xFF4D9FFF)   // синий диагонали N
val BrandCyan = Color(0xFF19E3D2)   // циан диагонали N
val Success   = Color(0xFF10B981)
val Warning   = Color(0xFFF59E0B)
val Danger    = Color(0xFFEF4444)
val Purple    = Color(0xFF8B5CF6)

// [+] Фирменный градиент (как диагональ N в иконке) —
//     для главной кнопки, заголовка, индикаторов
val BrandGradient = Brush.linearGradient(listOf(Accent, BrandCyan))

// ── Цветовые наборы по темам ─────────────────────────────────────────────────
private data class ThemeColors(
    val bgPrimary:    Color,
    val bgSecondary:  Color,
    val bgTertiary:   Color,
    val border:       Color,
    val textPrimary:  Color,
    val textSecondary: Color,
    val textMuted:    Color,
)

private val DarkColors = ThemeColors(
    // [*] В тон бренду: фон сплеша 0xFF0A0E14, подложка иконки 0xFF17222F
    bgPrimary     = Color(0xFF0A0E14),
    bgSecondary   = Color(0xFF11161D),
    bgTertiary    = Color(0xFF17222F),
    border        = Color(0xFF243140),
    textPrimary   = Color(0xFFE8EDF4),   // белый штрихов N
    textSecondary = Color(0xFF93A4B8),
    textMuted     = Color(0xFF5F6F80),
)

private val AmoledColors = ThemeColors(
    bgPrimary     = Color(0xFF000000),
    bgSecondary   = Color(0xFF0A0A0A),
    bgTertiary    = Color(0xFF141414),
    border        = Color(0xFF2A2A2A),
    textPrimary   = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFAAAAAA),
    textMuted     = Color(0xFF777777),
)

private val LightColors = ThemeColors(
    bgPrimary     = Color(0xFFF8FAFC),
    bgSecondary   = Color(0xFFFFFFFF),
    bgTertiary    = Color(0xFFF1F5F9),
    border        = Color(0xFFE2E8F0),
    textPrimary   = Color(0xFF0F172A),
    textSecondary = Color(0xFF334155),
    textMuted     = Color(0xFF64748B),
)

// ── CompositionLocal ─────────────────────────────────────────────────────────
// [*] CompositionLocal вместо глобальных mutableStateOf:
//     - рекомпозиция только у подписчиков внутри дерева, а не глобально
//     - нет side effects в теле Composable
//     - нет глобального синглтон-состояния, живущего весь процесс

private val LocalThemeColors = staticCompositionLocalOf { DarkColors }

// Публичные аксессоры — синтаксис использования в UI не меняется
val BgPrimary:     Color @Composable get() = LocalThemeColors.current.bgPrimary
val BgSecondary:   Color @Composable get() = LocalThemeColors.current.bgSecondary
val BgTertiary:    Color @Composable get() = LocalThemeColors.current.bgTertiary
val Border:        Color @Composable get() = LocalThemeColors.current.border
val TextPrimary:   Color @Composable get() = LocalThemeColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalThemeColors.current.textSecondary
val TextMuted:     Color @Composable get() = LocalThemeColors.current.textMuted

// ── Тема ─────────────────────────────────────────────────────────────────────
@Composable
fun MagiskModuleBuilderTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val themeMode = settings.themeMode

    val darkTheme = when (themeMode) {
        "light"  -> false
        "dark"   -> true
        "amoled" -> true
        else     -> isSystemInDarkTheme()
    }

    // [*] Выбираем набор цветов без side effects в теле Composable
    val themeColors = when (themeMode) {
        "amoled" -> AmoledColors
        "light"  -> LightColors
        else     -> DarkColors
    }

    val colorScheme = when {
        // [*] Dynamic Color отключён — он полностью игнорирует кастомную цветовую схему.
        //     На Android 12+ пользователь видел Material You вместо темы приложения.
        //     Если нужна поддержка Dynamic Color — вынести в отдельную настройку.
        darkTheme -> darkColorScheme(
            primary      = Accent,
            secondary    = themeColors.textSecondary,
            background   = themeColors.bgPrimary,
            // [*] surface = bgSecondary, а не хардкод BgPrimaryAmoled для всех тёмных тем
            surface      = themeColors.bgSecondary,
            onPrimary    = Color.White,
            onSecondary  = Color.Black,
            onBackground = themeColors.textPrimary,
            onSurface    = themeColors.textSecondary,
            error        = Danger
        )
        else -> lightColorScheme(
            primary      = Accent,
            secondary    = Accent,
            background   = themeColors.bgPrimary,
            surface      = themeColors.bgSecondary,
            onPrimary    = Color.White,
            onSecondary  = Color.White,
            onBackground = themeColors.textPrimary,
            onSurface    = themeColors.textSecondary,
            error        = Danger
        )
    }

    CompositionLocalProvider(LocalThemeColors provides themeColors) {
        MaterialTheme(colorScheme = colorScheme) {
            content()
        }
    }
}

// ── TextField цвета ──────────────────────────────────────────────────────────
// [*] Значения читаются в @Composable-контексте, затем передаются в colors()
//     без remember — OutlinedTextFieldDefaults.colors() сам @Composable,
//     его нельзя вызывать внутри remember { }
// [+] Форма полей ввода — крупный радиус для современного вида
val TextFieldShape = RoundedCornerShape(16.dp)

@Composable
fun textFieldColors(): TextFieldColors {
    val bgSecondary = BgSecondary
    val border      = Border
    val textMuted   = TextMuted
    val textPrimary = TextPrimary
    val accent      = Accent
    val brandCyan   = BrandCyan
    return OutlinedTextFieldDefaults.colors(
        // Текст
        focusedTextColor     = textPrimary,
        unfocusedTextColor   = textPrimary,
        // Фон поля: чуть светлее основного — поднимает элементы
        focusedContainerColor   = bgSecondary,
        unfocusedContainerColor = bgSecondary,
        // Рамка: акцент при фокусе, обычная граница без
        focusedBorderColor   = accent,
        unfocusedBorderColor = border,
        // Курсор и метки
        cursorColor          = brandCyan,
        focusedLabelColor    = accent,
        unfocusedLabelColor  = textMuted
    )
}