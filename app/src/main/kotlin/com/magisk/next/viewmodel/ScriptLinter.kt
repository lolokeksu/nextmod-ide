package com.magisk.next.viewmodel

import android.content.Context
import com.magisk.next.R

object ScriptLinter {

    enum class Severity { CRITICAL, DANGER, STYLE }

    data class Issue(
        val line: Int,
        val severity: Severity,
        val message: String
    )

    private data class Rule(
        val regex: Regex,
        val severity: Severity,
        val messageRes: Int
    )

    private val rules = listOf(
        Rule(Regex("""^\s*function\s+\w+"""), Severity.CRITICAL, R.string.lint_function_keyword),
        Rule(Regex("""\[\["""), Severity.CRITICAL, R.string.lint_double_bracket),
        Rule(Regex("""^\s*(declare|typeset)\s"""), Severity.CRITICAL, R.string.lint_declare),
        Rule(Regex("""<<<"""), Severity.CRITICAL, R.string.lint_herestring),
        Rule(Regex("""[<>]\("""), Severity.CRITICAL, R.string.lint_process_subst),
        Rule(Regex("""^\s*\w+=\(|\$\{\w+\["""), Severity.CRITICAL, R.string.lint_array),
        Rule(Regex("""\${"$"}\{\w+/"""), Severity.CRITICAL, R.string.lint_replace_expansion),
        Rule(Regex("""rm\s+(-\w*[rf]\w*\s+)+[^"'\s]*\${"$"}\{?\w+"""), Severity.DANGER, R.string.lint_unquoted_rm),
        Rule(Regex("""(dd\s+[^#\n]*of=|>+\s*)/dev/block"""), Severity.DANGER, R.string.lint_write_block),
        Rule(Regex("""mount\s+-o\s+[^#\n]*\brw\b[^#\n]*\s(/system|/vendor|/product)\b"""), Severity.DANGER, R.string.lint_remount_system),
        Rule(Regex("""chmod\s+(-R\s+)?0?777\b"""), Severity.DANGER, R.string.lint_chmod_777),
        Rule(Regex("""^\s*source\s"""), Severity.STYLE, R.string.lint_source_cmd),
        Rule(Regex("""^\s*let\s"""), Severity.STYLE, R.string.lint_let_cmd),
        Rule(Regex("""\${"$"}RANDOM\b"""), Severity.STYLE, R.string.lint_random)
    )

    private val funcHeader = Regex("""^\s*\w+\s*\(\)\s*\{?""")

    fun lint(context: Context, script: String): List<Issue> {
        if (script.isBlank()) return emptyList()

        val issues = mutableListOf<Issue>()
        var braceDepth = 0
        val funcStack = ArrayDeque<Int>()

        script.lines().forEachIndexed { index, raw ->
            val lineNo = index + 1

            if (lineNo == 1 && raw.startsWith("#!")) return@forEachIndexed

            val code = raw
                .replace(Regex("""(^|\s)#(?!\{).*$"""), "")
                .trimEnd()
            if (code.isBlank()) return@forEachIndexed

            val noStrings = stripQuotes(code)

            val isFuncHeader = funcHeader.containsMatchIn(noStrings)
            if (isFuncHeader && noStrings.contains('{')) {
                funcStack.addLast(braceDepth)
            }

            if (Regex("""^\s*local\s""").containsMatchIn(noStrings) && funcStack.isEmpty()) {
                issues += Issue(lineNo, Severity.CRITICAL,
                    context.getString(R.string.lint_local_outside_func))
            }

            for (rule in rules) {
                if (rule.regex.containsMatchIn(noStrings)) {
                    issues += Issue(lineNo, rule.severity,
                        context.getString(rule.messageRes))
                }
            }

            for (ch in noStrings) {
                when (ch) {
                    '{' -> braceDepth++
                    '}' -> {
                        // coerceAtLeast(0): защита от лишних } в кривом скрипте
                        braceDepth = (braceDepth - 1).coerceAtLeast(0)
                        if (funcStack.isNotEmpty() && braceDepth <= funcStack.last()) {
                            funcStack.removeLast()
                        }
                    }
                }
            }
        }

        return issues
    }

    private fun stripQuotes(line: String): String {
        val sb = StringBuilder(line.length)
        var inSingle = false
        var inDouble = false
        var escaped = false
        for (ch in line) {
            when {
                escaped -> { sb.append(' '); escaped = false }
                ch == '\\' && !inSingle -> { sb.append(' '); escaped = true }
                ch == '\'' && !inDouble -> { inSingle = !inSingle; sb.append(ch) }
                ch == '"' && !inSingle -> { inDouble = !inDouble; sb.append(ch) }
                inSingle || inDouble -> sb.append(' ')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}