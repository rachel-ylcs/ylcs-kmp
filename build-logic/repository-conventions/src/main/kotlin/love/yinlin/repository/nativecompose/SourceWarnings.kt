package love.yinlin.repository.nativecompose

import java.io.File

/** A source-only warning policy: maintained adaptation files never pass through this step. */
internal object SourceWarnings {
    // Bump when the prepared Kotlin source policy changes, including annotation placement.
    const val policy = 1
    private val extensions = setOf("kt", "kts")
    private const val argument = "\"warnings\", "

    fun trackedPaths(directory: File): Set<String> = sourceGit(directory, "ls-files", "-z").split('\u0000').filter {
        it.isNotEmpty() && it.substringAfterLast('.') in extensions && checkedSourcePath(directory, directory.resolve(sourceRelativePath(it))).isFile
    }.toSet()

    fun prepare(vendor: File, directory: File) {
        checkedSourcePath(vendor, directory)
        for (source in directory.walkTopDown().onEnter { it.name !in setOf(".git", ".gradle", ".kotlin", "build", "dependencies") }.filter { it.isFile && it.extension in extensions }) {
            val text = source.readText()
            val prepared = kotlin(text)
            if (prepared != text) writeSource(vendor, source, prepared.toByteArray())
        }
    }

    internal fun kotlin(text: String): String {
        val code = codeMask(text)
        val aliases = Regex("\\bimport\\s+kotlin\\.Suppress\\s+as\\s+(\\w+)").findAll(code).map { it.groupValues[1] }.toList()
        val name = "(?:kotlin\\s*\\.\\s*)?Suppress" + aliases.joinToString("") { "|${Regex.escape(it)}" }
        val direct = Regex("@file\\s*:\\s*(?:$name)\\s*\\(").find(code)?.range?.last
        var opening = direct
        if (opening == null) {
            for (group in Regex("@file\\s*:\\s*\\[").findAll(code)) {
                var depth = 1
                var end = group.range.last + 1
                while (end < code.length && depth > 0) {
                    if (code[end] == '[') depth++
                    else if (code[end] == ']') depth--
                    end++
                }
                val match = Regex("(?<![\\w.])(?:$name)\\s*\\(").find(code.substring(group.range.last + 1, end))
                if (match != null) {
                    opening = group.range.last + 1 + match.range.last
                    break
                }
            }
        }
        if (opening != null) {
            // Also support named vararg arrays without introducing a duplicate argument.
            val named = Regex("\\s*names\\s*=\\s*(?:\\[|(?:kotlin\\.)?arrayOf\\s*\\()").find(code, opening + 1)?.takeIf { it.range.first == opening + 1 }
            val position = if (named == null) opening + 1 else named.range.last + 1
            if (text.startsWith("\"warnings\"", position)) return text
            return text.substring(0, position) + argument + text.substring(position)
        }
        val position = code.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: text.length
        val newline = if ("\r\n" in text) "\r\n" else "\n"
        val separator = if (position > 0 && text[position - 1] !in "\r\n\uFEFF") newline else ""
        return text.substring(0, position) + separator + "@file:kotlin.Suppress(\"warnings\")" + newline + text.substring(position)
    }

    /** Preserve offsets while hiding comments/literals, including nested comments and script headers. */
    private fun codeMask(text: String): String {
        val result = text.toCharArray()
        var index = if (text.startsWith('\uFEFF')) 1 else 0
        if (index == 1) result[0] = ' '
        while (index < text.length) {
            val start = index
            when {
                text.startsWith("//", index) || (index <= 1 && text.startsWith("#!", index)) -> {
                    index = text.indexOf('\n', index).takeIf { it >= 0 } ?: text.length
                }
                text.startsWith("/*", index) -> {
                    index += 2
                    var depth = 1
                    while (index < text.length && depth > 0) {
                        when {
                            text.startsWith("/*", index) -> {
                                depth++
                                index += 2
                            }
                            text.startsWith("*/", index) -> {
                                depth--
                                index += 2
                            }
                            else -> index++
                        }
                    }
                }
                text.startsWith("\"\"\"", index) -> {
                    index = text.indexOf("\"\"\"", index + 3).takeIf { it >= 0 }?.plus(3) ?: text.length
                }
                text[index] == '"' || text[index] == '\'' -> {
                    val quote = text[index++]
                    while (index < text.length) {
                        val character = text[index++]
                        if (character == '\\' && index < text.length) index++
                        else if (character == quote) break
                    }
                }
                else -> {
                    index++
                    continue
                }
            }
            for (position in start ..< index) if (result[position] !in "\r\n") result[position] = ' '
        }
        return result.concatToString()
    }
}
