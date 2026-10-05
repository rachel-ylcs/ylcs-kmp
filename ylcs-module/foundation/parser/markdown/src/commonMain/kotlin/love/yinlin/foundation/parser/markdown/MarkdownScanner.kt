package love.yinlin.foundation.parser.markdown

internal data class MarkdownLinkTarget(val destination: String, val title: String? = null)
internal data class MarkdownScan(val value: String, val end: Int)

internal fun markdownPunctuation(c: Char): Boolean = c in '!'..'/' || c in ':'..'@' || c in '['..'`' || c in '{'..'~'
internal fun markdownSpace(c: Char): Boolean = c == ' ' || c == '\t' || c == '\n' || c == '\r'

internal fun markdownReference(label: String): String = buildString {
    var space = false
    for (c in label.trim()) {
        if (c.isWhitespace()) space = true
        else {
            if (space && isNotEmpty()) append(' ')
            append(c.lowercaseChar())
            space = false
        }
    }
}

internal fun scanMarkdownDestination(text: String, start: Int): MarkdownScan? {
    if (text.getOrNull(start) == '<') {
        var i = start + 1
        while (i < text.length) {
            when (text[i]) {
                '\\' -> if (i + 1 < text.length && markdownPunctuation(text[i + 1])) i++
                '>' -> return MarkdownScan(decodeMarkdownText(text.substring(start + 1, i)), i + 1)
                '<', '\n', '\r' -> return null
            }
            i++
        }
        return null
    }
    var i = start
    var depth = 0
    while (i < text.length) {
        val c = text[i]
        if (c <= ' ' || c == '\u007f') break
        if (c == '\\' && i + 1 < text.length && markdownPunctuation(text[i + 1])) i++
        else if (c == '(') { if (++depth > 64) return null }
        else if (c == ')') { if (depth == 0) break; depth-- }
        i++
    }
    if (depth != 0) return null
    return MarkdownScan(decodeMarkdownText(text.substring(start, i)), i)
}

internal fun scanMarkdownTitle(text: String, start: Int): MarkdownScan? {
    val open = text.getOrNull(start) ?: return null
    val close = when (open) { '"', '\'' -> open; '(' -> ')'; else -> return null }
    var i = start + 1
    while (i < text.length) {
        if (text[i] == '\\' && i + 1 < text.length && markdownPunctuation(text[i + 1])) i++
        else if (text[i] == close) return MarkdownScan(decodeMarkdownText(text.substring(start + 1, i)), i + 1)
        else if (text[i] == '(' && open == '(') return null
        else if (text[i] == '\n' && text.getOrNull(i + 1) == '\n') return null
        i++
    }
    return null
}

internal fun decodeMarkdownText(text: String): String {
    if ('\\' !in text && '&' !in text) return text
    return buildString(text.length) {
        var i = 0
        while (i < text.length) {
            if (text[i] == '\\' && i + 1 < text.length && markdownPunctuation(text[i + 1])) append(text[++i])
            else if (text[i] == '&') {
                val entity = scanMarkdownEntity(text, i)
                if (entity != null) { append(entity.value); i = entity.end; continue }
                append('&')
            } else append(text[i])
            i++
        }
    }
}

private val markdownEntities = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00a0",
    "copy" to "©", "reg" to "®", "trade" to "™", "ndash" to "–", "mdash" to "—", "hellip" to "…",
    "laquo" to "«", "raquo" to "»", "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”",
    "bull" to "•", "middot" to "·", "euro" to "€", "pound" to "£", "yen" to "¥", "cent" to "¢",
    "times" to "×", "divide" to "÷", "plusmn" to "±", "minus" to "−", "deg" to "°", "micro" to "µ",
    "AElig" to "Æ", "aelig" to "æ", "OElig" to "Œ", "oelig" to "œ", "szlig" to "ß",
    "eacute" to "é", "Eacute" to "É", "ouml" to "ö", "uuml" to "ü", "auml" to "ä",
    "colon" to ":", "semi" to ";", "sol" to "/", "bsol" to "\\", "lpar" to "(", "rpar" to ")",
    "lsqb" to "[", "rsqb" to "]", "lcub" to "{", "rcub" to "}", "ast" to "*", "lowbar" to "_",
    "num" to "#", "period" to ".", "comma" to ",", "excl" to "!", "quest" to "?", "equals" to "=",
    "vert" to "|", "verbar" to "|", "grave" to "`", "tilde" to "~", "Tab" to "\t", "NewLine" to "\n",
)

internal fun scanMarkdownEntity(text: String, start: Int): MarkdownScan? {
    val limit = minOf(text.length, start + 34)
    var end = start + 1
    while (end < limit && text[end] != ';' && text[end] != '&' && !text[end].isWhitespace()) end++
    if (end >= limit || text[end] != ';') return null
    val name = text.substring(start + 1, end)
    val value = if (name.startsWith('#')) {
        val hex = name.getOrNull(1) == 'x' || name.getOrNull(1) == 'X'
        val digits = name.substring(if (hex) 2 else 1)
        if (digits.isEmpty() || digits.length > if (hex) 6 else 7) return null
        val number = digits.toIntOrNull(if (hex) 16 else 10) ?: return null
        if (number == 0 || number > 0x10ffff || number in 0xd800..0xdfff) "\ufffd"
        else if (number <= 0xffff) number.toChar().toString()
        else {
            val code = number - 0x10000
            "${(0xd800 + (code ushr 10)).toChar()}${(0xdc00 + (code and 1023)).toChar()}"
        }
    } else markdownEntities[name] ?: return null
    return MarkdownScan(value, end + 1)
}

internal class MarkdownParseContext(val source: String, val config: MarkdownConfiguration) {
    private var nodes = 0

    fun depth(depth: Int, offset: Int) {
        if (depth > config.maxDepth) fail("Maximum Markdown nesting depth exceeded", offset)
    }

    fun node(depth: Int, offset: Int) {
        depth(depth, offset)
        if (++nodes > config.maxNodes) fail("Markdown node limit exceeded", offset)
    }

    fun fail(message: String, offset: Int): Nothing {
        val position = offset.coerceIn(0, source.length)
        var line = 1
        var column = 1
        var i = 0
        while (i < position) {
            if (source[i] == '\r') {
                line++; column = 1
                if (source.getOrNull(i + 1) == '\n' && i + 1 < position) i++
            } else if (source[i] == '\n') { line++; column = 1 }
            else column++
            i++
        }
        throw MarkdownParseException(message, line, column, position)
    }
}
