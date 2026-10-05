package love.yinlin.foundation.parser.yaml

internal class YamlWriter(private val config: YamlConfiguration) {
    private val output = StringBuilder()
    private var nodes = 0

    fun write(value: YamlElement): String {
        if (config.style == YamlStyle.Flow) writeFlow(value, 0)
        else writeBlock(value, 0, 0)
        return output.toString()
    }

    private fun visit(depth: Int) {
        require(depth <= config.maxDepth) { "Maximum YAML nesting depth exceeded while writing" }
        require(++nodes <= config.maxNodes) { "YAML node limit exceeded while writing" }
    }

    private fun writeBlock(value: YamlElement, indent: Int, depth: Int) {
        visit(depth)
        when (value) {
            is YamlObject if value.isNotEmpty() -> for ([key, child] in value) {
                spaces(indent)
                writeString(key, flow = false, key = true)
                output.append(':')
                writeChild(child, indent, depth + 1)
            }

            is YamlArray if value.isNotEmpty() -> for (child in value) {
                spaces(indent)
                output.append('-')
                writeChild(child, indent, depth + 1)
            }

            else -> {
                spaces(indent)
                if (canUseLiteral(value)) writeLiteral((value as YamlPrimitive).content, indent)
                else { writeInline(value); output.append('\n') }
            }
        }
    }

    private fun writeChild(value: YamlElement, indent: Int, depth: Int) {
        if ((value is YamlObject && value.isNotEmpty()) || (value is YamlArray && value.isNotEmpty())) {
            output.append('\n')
            writeBlock(value, indent + config.indent, depth)
        } else {
            visit(depth)
            output.append(' ')
            if (canUseLiteral(value)) writeLiteral((value as YamlPrimitive).content, indent)
            else { writeInline(value); output.append('\n') }
        }
    }

    private fun writeInline(value: YamlElement) = when (value) {
        is YamlObject -> { check(value.isEmpty()); output.append("{}") }
        is YamlArray -> { check(value.isEmpty()); output.append("[]") }
        is YamlPrimitive -> writePrimitive(value, flow = false)
    }

    private fun writeFlow(value: YamlElement, depth: Int) {
        visit(depth)
        when (value) {
            is YamlObject -> {
                output.append('{')
                var first = true
                for ([key, child] in value) {
                    if (!first) output.append(", ")
                    first = false
                    writeString(key, flow = true, key = true)
                    output.append(": ")
                    writeFlow(child, depth + 1)
                }
                output.append('}')
            }
            is YamlArray -> {
                output.append('[')
                for (i in value.indices) {
                    if (i != 0) output.append(", ")
                    writeFlow(value[i], depth + 1)
                }
                output.append(']')
            }
            is YamlPrimitive -> writePrimitive(value, flow = true)
        }
    }

    private fun writePrimitive(value: YamlPrimitive, flow: Boolean) {
        if (value.isString) writeString(value.content, flow = flow, key = false)
        else output.append(value.content)
    }

    private fun canUseLiteral(value: YamlElement): Boolean = config.blockStrings &&
        value is YamlPrimitive && value.isString && '\n' in value.content &&
        value.content.any { it != '\n' && it != ' ' && it != '\t' } &&
        value.content.none { (it < ' ' && it != '\n' && it != '\t') || it in '\u007f'..'\u009f' || it == '\ufffe' || it == '\uffff' }

    private fun writeLiteral(text: String, indent: Int) {
        var trailing = 0
        var i = text.lastIndex
        while (i >= 0 && text[i--] == '\n') trailing++
        output.append('|').append(config.indent)
        if (trailing == 0) output.append('-') else if (trailing > 1) output.append('+')
        output.append('\n')
        var start = 0
        while (start < text.length) {
            val end = text.indexOf('\n', start).let { if (it == -1) text.length else it }
            spaces(indent + config.indent)
            output.append(text, start, end).append('\n')
            start = end + 1
        }
    }

    private fun writeString(text: String, flow: Boolean, key: Boolean) {
        if (isPlain(text, flow, key)) { output.append(text); return }
        output.append('"')
        for (c in text) {
            when (c) {
                '"' -> output.append("\\\"")
                '\\' -> output.append("\\\\")
                '\n' -> output.append("\\n")
                '\r' -> output.append("\\r")
                '\t' -> output.append("\\t")
                '\b' -> output.append("\\b")
                '\u000c' -> output.append("\\f")
                else -> if (c < ' ' || c in '\u007f'..'\u009f' || c == '\ufffe' || c == '\uffff') {
                    output.append("\\u")
                    for (shift in 12 downTo 0 step 4) output.append("0123456789abcdef"[(c.code ushr shift) and 15])
                } else output.append(c)
            }
        }
        output.append('"')
    }

    private fun isPlain(text: String, flow: Boolean, key: Boolean): Boolean {
        if (text.isEmpty() || text.first().isWhitespace() || text.last().isWhitespace()) return false
        if (key && text == "<<") return false
        if (YamlScalarKind.fromString(text) != YamlScalarKind.String) return false
        if (text[0] in "-?:,[]{}#&*!|>'\"%@`") return false
        if (text == "---" || text == "...") return false
        for (i in text.indices) {
            val c = text[i]
            if (c < ' ' || c in '\u007f'..'\u009f' || c == '\ufffe' || c == '\uffff' ||
                c == '\u2028' || c == '\u2029' || c == '\ufeff') return false
            if (flow && c in "[]{},:") return false
            if (c == ':' && (i == text.lastIndex || text[i + 1].isWhitespace())) return false
            if (c == '#' && (i == 0 || text[i - 1].isWhitespace())) return false
        }
        return true
    }

    private fun spaces(count: Int) { repeat(count) { output.append(' ') } }
}