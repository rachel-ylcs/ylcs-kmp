package love.yinlin.foundation.parser.toml

internal class TomlParser(private val source: String, private val configuration: TomlConfiguration) {
    private var position = 0
    private var nodes = 0

    private sealed interface Node

    private enum class Definition { Root, Implicit, Header, Dotted, Inline }

    private class Table(val depth: Int, var definition: Definition, var owner: Table? = null) : Node {
        val entries = linkedMapOf<String, Node>()
        var sealed = false
    }

    private class ArrayNode(val depth: Int, val tableArray: Boolean = false) : Node {
        val entries: MutableList<Node> = []
    }

    private class Scalar(val value: TomlPrimitive) : Node

    fun parse(): TomlObject {
        val root = table(0, Definition.Root)
        var current = root
        skipTrivia(true)
        while (position < source.length) {
            if (source[position] == '[') current = header(root)
            else assignment(current)
            finishLine()
            skipTrivia(true)
        }
        return freeze(root).Object
    }

    private fun header(root: Table): Table {
        ++position
        val array = take('[')
        val path = keyPath()
        expect(']')
        if (array) expect(']')
        var parent = root
        for (i in 0 until path.lastIndex) {
            val name = path[i]
            parent = when (val node = parent.entries[name]) {
                null -> table(parent.depth + 1, Definition.Implicit).also { parent.entries[name] = it }
                is Table -> node.also { if (it.sealed) fail("Cannot extend an inline table") }
                is ArrayNode -> if (node.tableArray) node.entries.last() as Table
                    else fail("Cannot use an ordinary array as a table")
                else -> fail("Cannot use '$name' as a table")
            }
        }
        val name = path.last()
        val previous = parent.entries[name]
        if (array) {
            val list = when (previous) {
                null -> array(parent.depth + 1, true).also { parent.entries[name] = it }
                is ArrayNode -> if (previous.tableArray) previous else fail("Cannot append to an ordinary array")
                else -> fail("Cannot redefine '$name' as an array of tables")
            }
            return table(list.depth + 1, Definition.Header).also { list.entries.add(it) }
        }
        return when (previous) {
            null -> table(parent.depth + 1, Definition.Header).also { parent.entries[name] = it }
            is Table -> {
                if (previous.sealed || previous.definition != Definition.Implicit) fail("Table '$name' is already defined")
                previous.definition = Definition.Header
                previous
            }
            else -> fail("Cannot redefine '$name' as a table")
        }
    }

    private fun assignment(context: Table) {
        val path = keyPath()
        expect('=')
        skipHorizontal()
        var parent = context
        for (i in 0 until path.lastIndex) {
            val name = path[i]
            parent = when (val node = parent.entries[name]) {
                null -> table(parent.depth + 1, Definition.Dotted, context).also { parent.entries[name] = it }
                is Table -> {
                    if (node.sealed || node.definition == Definition.Header ||
                        node.definition == Definition.Dotted && node.owner !== context) {
                        fail("Cannot redefine table '$name' through a dotted key")
                    }
                    if (node.definition == Definition.Implicit) {
                        node.definition = Definition.Dotted
                        node.owner = context
                    }
                    node
                }
                else -> fail("Cannot use '$name' as a table in a dotted key")
            }
        }
        val name = path.last()
        if (parent.entries.containsKey(name)) fail("Key '$name' is already defined")
        parent.entries[name] = value(parent.depth + 1)
    }

    private fun keyPath(): List<String> {
        val path: MutableList<String> = []
        while (true) {
            skipHorizontal()
            if (position >= source.length) fail("Expected a key")
            val key = if (source[position] == '"' || source[position] == '\'') string(true) else {
                val start = position
                while (position < source.length && TomlScalarKind.isBareKey(source[position])) ++position
                if (position == start) fail("Expected a bare or quoted key")
                source.substring(start, position)
            }
            path.add(key)
            if (path.size > configuration.maxDepth + 1) fail("Maximum TOML depth exceeded")
            skipHorizontal()
            if (!take('.')) return path
        }
    }

    private fun value(depth: Int): Node {
        if (position >= source.length) fail("Expected a TOML value")
        return when (source[position]) {
            '"', '\'' -> {
                count(depth)
                Scalar(TomlLiteral(string(false), TomlScalarKind.String))
            }
            '[' -> arrayValue(depth)
            '{' -> inlineTable(depth)
            else -> scalar(depth)
        }
    }

    private fun scalar(depth: Int): Node {
        count(depth)
        val start = position
        while (position < source.length && !valueEnd(source[position])) ++position
        // RFC 3339 permits exactly one space in place of the date/time separator.
        if (position - start == 10 && source.getOrNull(start + 4) == '-' &&
            source.getOrNull(start + 7) == '-' && source.getOrNull(position) == ' ' &&
            source.getOrNull(position + 1) in '0'..'9') {
            ++position
            while (position < source.length && !valueEnd(source[position])) ++position
        }
        val text = source.substring(start, position)
        val kind = when (text) {
            "true", "false" -> TomlScalarKind.Boolean
            else -> TomlScalarKind.numberKind(text) ?: TomlScalarKind.dateTimeKind(text, configuration.version)
        } ?: fail("Invalid TOML value '$text'", start)
        return Scalar(TomlLiteral(text, kind))
    }

    private fun valueEnd(char: Char): Boolean = when (char) {
        ' ', '\t', '\n', '\r', '#', ',', ']', '}' -> true
        else -> false
    }

    private fun arrayValue(depth: Int): ArrayNode {
        val result = array(depth)
        ++position
        skipTrivia(true)
        if (take(']')) return result
        while (true) {
            result.entries.add(value(depth + 1))
            skipTrivia(true)
            if (take(']')) return result
            expect(',')
            skipTrivia(true)
            if (take(']')) return result
        }
    }

    private fun inlineTable(depth: Int): Table {
        val result = table(depth, Definition.Inline)
        ++position
        val multiline = configuration.version == TomlVersion.V1_1
        skipTrivia(multiline)
        if (!take('}')) {
            while (true) {
                assignment(result)
                skipTrivia(multiline)
                if (take('}')) break
                expect(',')
                skipTrivia(multiline)
                if (take('}')) {
                    if (!multiline) fail("Trailing commas in inline tables require TOML 1.1")
                    break
                }
            }
        }
        result.sealed = true
        return result
    }

    private fun string(key: Boolean): String {
        val quote = source[position]
        val basic = quote == '"'
        val multiline = source.getOrNull(position + 1) == quote && source.getOrNull(position + 2) == quote
        if (key && multiline) fail("Multiline strings cannot be keys")
        position += if (multiline) 3 else 1
        if (multiline && (source.getOrNull(position) == '\n' || source.getOrNull(position) == '\r')) newline()
        var start = position
        var builder: StringBuilder? = null
        while (position < source.length) {
            val char = source[position]
            if (char == quote) {
                val end = position
                if (!multiline) {
                    ++position
                    return builder?.append(source, start, end)?.toString() ?: source.substring(start, end)
                }
                while (position < source.length && source[position] == quote) ++position
                val quotes = position - end
                if (quotes >= 3) {
                    if (quotes > 5) fail("Too many closing quotes", end)
                    val contentEnd = end + quotes - 3
                    return builder?.append(source, start, contentEnd)?.toString() ?: source.substring(start, contentEnd)
                }
                continue
            }
            if (basic && char == '\\') {
                val output = builder ?: StringBuilder().also { builder = it }
                output.append(source, start, position)
                ++position
                val next = source.getOrNull(position)
                if (multiline && (next == ' ' || next == '\t' || next == '\n' || next == '\r')) {
                    skipHorizontal()
                    if (source.getOrNull(position) != '\n' && source.getOrNull(position) != '\r')
                        fail("A multiline continuation must end with a newline")
                    newline()
                    while (position < source.length) {
                        skipHorizontal()
                        if (source.getOrNull(position) != '\n' && source.getOrNull(position) != '\r') break
                        newline()
                    }
                } else escape(output)
                start = position
                continue
            }
            if (char == '\n' || char == '\r') {
                if (!multiline) fail("Newline in a single-line string")
                if (char == '\r') {
                    val output = builder ?: StringBuilder().also { builder = it }
                    output.append(source, start, position)
                    newline()
                    output.append('\n')
                    start = position
                } else ++position
                continue
            }
            if (char < ' ' && char != '\t' || char == '\u007f') fail("Unescaped control character in string")
            unicodeChar()
        }
        fail("Unterminated TOML string")
    }

    private fun escape(output: StringBuilder) {
        if (position >= source.length) fail("Unterminated escape")
        when (val char = source[position++]) {
            'b' -> output.append('\b')
            't' -> output.append('\t')
            'n' -> output.append('\n')
            'f' -> output.append('\u000c')
            'r' -> output.append('\r')
            '"' -> output.append('"')
            '\\' -> output.append('\\')
            'e' -> {
                if (configuration.version == TomlVersion.V1_0) fail("\\e requires TOML 1.1", position - 1)
                output.append('\u001b')
            }
            'x', 'u', 'U' -> {
                if (char == 'x' && configuration.version == TomlVersion.V1_0) fail("\\x requires TOML 1.1", position - 1)
                val length = when (char) { 'x' -> 2; 'u' -> 4; else -> 8 }
                var codePoint = 0L
                repeat(length) {
                    val digit = source.getOrNull(position)?.let { TomlScalarKind.digit(it) } ?: -1
                    if (digit < 0) fail("Expected $length hexadecimal digits in Unicode escape")
                    codePoint = codePoint * 16 + digit
                    ++position
                }
                if (codePoint > 0x10ffff || codePoint in 0xd800..0xdfff) fail("Invalid Unicode scalar value")
                if (codePoint <= 0xffff) output.append(codePoint.toInt().toChar()) else {
                    val value = codePoint.toInt() - 0x10000
                    output.append((0xd800 + (value shr 10)).toChar())
                    output.append((0xdc00 + (value and 0x3ff)).toChar())
                }
            }
            else -> fail("Invalid TOML escape \\$char", position - 1)
        }
    }

    /** Validate UTF-16 as it is consumed, avoiding a second pass over the source. */
    private fun unicodeChar() {
        val char = source[position++]
        if (char in '\ud800'..'\udbff') {
            if (source.getOrNull(position) !in '\udc00'..'\udfff') fail("Unpaired Unicode surrogate", position - 1)
            ++position
        } else if (char in '\udc00'..'\udfff') fail("Unpaired Unicode surrogate", position - 1)
    }

    private fun skipHorizontal() {
        while (position < source.length && (source[position] == ' ' || source[position] == '\t')) ++position
    }

    private fun skipTrivia(multiline: Boolean) {
        while (true) {
            skipHorizontal()
            if (!multiline || position == source.length) return
            when (source[position]) {
                '#' -> comment()
                '\n', '\r' -> newline()
                else -> return
            }
        }
    }

    private fun comment() {
        ++position
        while (position < source.length && source[position] != '\n' && source[position] != '\r') {
            val char = source[position]
            if (char < ' ' && char != '\t' || char == '\u007f') fail("Control character in comment")
            unicodeChar()
        }
    }

    private fun newline() {
        if (source[position++] == '\r' && !take('\n')) fail("A carriage return must be followed by a line feed", position - 1)
    }

    private fun finishLine() {
        skipHorizontal()
        if (source.getOrNull(position) == '#') comment()
        if (position == source.length) return
        if (source[position] != '\n' && source[position] != '\r') fail("Expected a newline after a key/value pair or header")
        newline()
    }

    private fun take(char: Char): Boolean = if (source.getOrNull(position) == char) {
        ++position
        true
    } else false

    private fun expect(char: Char) { if (!take(char)) fail("Expected '$char'") }

    private fun count(depth: Int) {
        if (depth > configuration.maxDepth) fail("Maximum TOML depth exceeded")
        if (++nodes > configuration.maxNodes) fail("Maximum TOML node count exceeded")
    }

    private fun table(depth: Int, definition: Definition, owner: Table? = null): Table {
        count(depth)
        return Table(depth, definition, owner)
    }

    private fun array(depth: Int, tableArray: Boolean = false): ArrayNode {
        count(depth)
        return ArrayNode(depth, tableArray)
    }

    private fun freeze(node: Node): TomlElement = when (node) {
        is Scalar -> node.value
        is Table -> {
            val result = LinkedHashMap<String, TomlElement>(node.entries.size)
            for ((key, value) in node.entries) result[key] = freeze(value)
            TomlObject.owned(result)
        }
        is ArrayNode -> {
            val result = ArrayList<TomlElement>(node.entries.size)
            for (value in node.entries) result.add(freeze(value))
            TomlArray.owned(result)
        }
    }

    private fun fail(message: String, offset: Int = position): Nothing {
        var line = 1
        var lineStart = 0
        for (i in 0 until offset) if (source[i] == '\n') {
            ++line
            lineStart = i + 1
        }
        throw TomlParseException(message, line, offset - lineStart + 1, offset)
    }
}