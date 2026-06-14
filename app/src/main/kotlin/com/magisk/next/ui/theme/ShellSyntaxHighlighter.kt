package com.magisk.next.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ShellSyntaxHighlighter : VisualTransformation {

    companion object {
        private val KEYWORD_COLOR  = Color(0xFFFF79C6)
        private val STRING_COLOR   = Color(0xFFF1FA8C)
        private val COMMENT_COLOR  = Color(0xFF6272A4)
        private val VARIABLE_COLOR = Color(0xFF8BE9FD)

        private val VAR_PATTERN    = Regex("""\$[a-zA-Z_][a-zA-Z0-9_]*""")
        private val STRING_PATTERN = Regex(""""[^"]*"|'[^']*'""")

        // [*] Set вместо List — проверка вхождения O(1), а не O(K) перебором
        private val KEYWORDS = setOf(
            "ui_print", "set_perm", "set_perm_recursive", "chmod", "chown",
            "mount", "umount", "cp", "mv", "mkdir", "rm", "echo", "exit",
            "if", "then", "else", "elif", "fi", "while", "do", "done",
            "for", "in", "case", "esac", "function", "return", "sleep",
            "log", "abort", "getprop", "setprop", "resetprop"
        )
        // Разбивка на токены-слова с сохранением позиций
        private val WORD_PATTERN = Regex("""[a-zA-Z_][a-zA-Z0-9_]*""")

        private val KEYWORD_STYLE  = SpanStyle(color = KEYWORD_COLOR, fontWeight = FontWeight.Bold)
        private val VARIABLE_STYLE = SpanStyle(color = VARIABLE_COLOR)
        private val STRING_STYLE   = SpanStyle(color = STRING_COLOR)
        private val COMMENT_STYLE  = SpanStyle(color = COMMENT_COLOR, fontStyle = FontStyle.Italic)
    }

    // [*] Кеш последнего результата: filter() вызывается на каждый кадр
    //     (скролл/курсор/фокус), но текст меняется редко — пересчёт только при изменении
    private var cachedInput: String? = null
    private var cachedResult: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        cachedResult?.let { if (cachedInput == text.text) return it }

        val builder = AnnotatedString.Builder(text.text)
        var currentIndex = 0
        for (line in text.text.lines()) {
            val hashPos = line.indexOf("#")
            val trimmed = line.trimStart()

            if (trimmed.startsWith("#")) {
                // Вся строка — комментарий
                builder.addStyle(COMMENT_STYLE, currentIndex + line.indexOf("#"), currentIndex + line.length)
            } else {
                // Граница кода: до символа # (если есть инлайн-комментарий)
                val codeEnd = if (hashPos >= 0) hashPos else line.length
                highlightCode(line, codeEnd, builder, currentIndex)
                if (hashPos >= 0) {
                    builder.addStyle(COMMENT_STYLE, currentIndex + hashPos, currentIndex + line.length)
                }
            }
            currentIndex += line.length + 1
        }

        val result = TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        cachedInput = text.text
        cachedResult = result
        return result
    }

    private fun highlightCode(line: String, codeEnd: Int, builder: AnnotatedString.Builder, lineOffset: Int) {
        // [*] Один проход токенизации: каждое слово проверяется по Set за O(1)
        for (match in WORD_PATTERN.findAll(line.substring(0, codeEnd))) {
            if (match.value in KEYWORDS) {
                builder.addStyle(KEYWORD_STYLE, lineOffset + match.range.first, lineOffset + match.range.last + 1)
            }
        }
        // Переменные и строки — в пределах кодовой части
        val codePart = line.substring(0, codeEnd)
        for (match in VAR_PATTERN.findAll(codePart)) {
            builder.addStyle(VARIABLE_STYLE, lineOffset + match.range.first, lineOffset + match.range.last + 1)
        }
        for (match in STRING_PATTERN.findAll(codePart)) {
            builder.addStyle(STRING_STYLE, lineOffset + match.range.first, lineOffset + match.range.last + 1)
        }
    }
}
