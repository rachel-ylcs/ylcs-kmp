package love.yinlin.foundation.parser.yaml

internal class YamlParser(private val source: String, private val config: YamlConfiguration) {
    private data class Line(val start: Int, val end: Int, val next: Int, val content: Int, val indent: Int)
    private data class Properties(val tag: String?, val anchor: String?)
    private data class BlockPart(val start: Int, val end: Int, val hasBreak: Boolean, val moreIndented: Boolean)

    private val lines: List<Line> = indexLines()
    private var lineIndex = 0
    private var nodes = 0
    private var aliases = 0
    private var locatedLine = 0
    private val anchors = mutableMapOf<String, YamlElement>()
    private val pendingAnchors = mutableSetOf<String>()

    fun parse(): YamlElement {
        skipTrivia()
        var directive = false
        while (lineIndex < lines.size && charAt(lines[lineIndex].content) == '%') {
            (val end, val content, val indent) = lines[lineIndex]
            if (indent != 0) fail("Directives must start in column 1", content)
            val value = source.substring(content, commentEnd(content, end)).trimEnd()
            if (value != "%YAML 1.2") fail("Only the %YAML 1.2 directive is supported", content)
            if (directive) fail("Duplicate YAML directive", content)
            directive = true
            lineIndex++
            skipTrivia()
        }
        val marker = lineIndex < lines.size && isMarker(lines[lineIndex], "---")
        if (directive && !marker) fail("A directive must be followed by ---", currentOffset())
        val value = if (marker) {
            (val end, val content) = lines[lineIndex]
            val start = spaces(content + 3, end)
            if (start < end && source[start] != '#') {
                if (mappingColon(start, end) >= 0) fail("A block mapping must start after the document marker line", start)
                readValue(start, -1, 0, compact = true)
            } else {
                lineIndex++
                skipTrivia()
                readDocumentRoot()
            }
        } else readDocumentRoot()
        skipTrivia()
        if (lineIndex < lines.size && isMarker(lines[lineIndex], "...")) {
            (val end1 = end, val content) = lines[lineIndex]
            val extra = spaces(content + 3, end1)
            if (extra < end1 && source[extra] != '#') fail("Unexpected content after ...", extra)
            lineIndex++
            skipTrivia()
        }
        if (lineIndex < lines.size) {
            val line = lines[lineIndex]
            if (isDocumentBoundary(line) || charAt(line.content) == '%') fail("Expected one YAML document", line.content)
            fail("Unexpected content or indentation", line.content)
        }
        return value
    }

    private fun readDocumentRoot(): YamlElement {
        if (lineIndex == lines.size || isDocumentBoundary(lines[lineIndex])) return YamlNull
        return readBlock(0)
    }

    private fun readBlock(depth: Int): YamlElement {
        val line = lines[lineIndex]
        checkIndent(line)
        return readValue(line.content, line.indent - 1, depth, compact = true)
    }

    private fun readValue(
        start: Int, parentIndent: Int, depth: Int,
        compact: Boolean = false, indentlessSequence: Boolean = false,
    ): YamlElement {
        checkNode(depth, start)
        (val end, val content, val indent) = lines[lineIndex]
        if (compact && !isSequence(start) && mappingColon(start, end) >= 0) {
            return readMapping(start - content + indent, start, depth)
        }
        val reader = FlowReader(start, parentIndent)
        val props = reader.properties(flow = false)
        beginAnchor(props.anchor, start)
        (val end1 = end, val content1 = content, val indent1 = indent) = lines[lineIndex]
        val position = reader.position
        if (charAt(position) == '*' && (props.tag != null || props.anchor != null)) {
            fail("An alias cannot have tags or anchors", position)
        }
        val value = when {
            position >= end1 || source[position] == '#' -> {
                lineIndex++
                skipTrivia()
                if (lineIndex < lines.size && !isDocumentBoundary(lines[lineIndex]) &&
                    (lines[lineIndex].indent > parentIndent ||
                        (indentlessSequence && lines[lineIndex].indent == parentIndent && isSequence(lines[lineIndex].content)))) {
                    if ((props.tag != null || props.anchor != null) &&
                        (charAt(lines[lineIndex].content) == '!' || charAt(lines[lineIndex].content) == '&')) {
                        fail("Multiple lines of node properties are not supported", lines[lineIndex].content)
                    }
                    readBlock(depth)
                } else if (props.tag == "str") YamlPrimitive("") else YamlNull
            }
            source[position] == '|' || source[position] == '>' -> readBlockScalar(position, parentIndent)
            compact && isSequence(position) -> {
                if (props.tag != null || props.anchor != null) fail("Put properties of a block sequence on a preceding line", start)
                readSequence(position - content1 + indent1, position, depth)
            }
            compact && mappingColon(position, end1) >= 0 -> readMapping(position - content1 + indent1, position, depth)
            else -> {
                val element = reader.body(depth, flow = false, forceString = props.tag == "str")
                reader.finishLine()
                element
            }
        }
        return finishProperties(value, props, start)
    }

    private fun readSequence(indent: Int, first: Int, depth: Int): YamlArray {
        val result: MutableList<YamlElement> = []
        var start = first
        while (true) {
            val line = lines[lineIndex]
            checkIndent(line)
            val valueStart = spaces(start + 1, line.end)
            if ((isSequence(valueStart) || mappingColon(valueStart, line.end) >= 0) &&
                source.substring(start + 1, valueStart).contains('\t')) {
                fail("Tabs cannot indent a compact block collection", start + 1)
            }
            result.add(readValue(valueStart, indent, depth + 1, compact = true))
            skipTrivia()
            if (lineIndex == lines.size || isDocumentBoundary(lines[lineIndex])) break
            (val content, val indent1 = indent) = lines[lineIndex]
            if (indent1 > indent) fail("Unexpected indentation after a sequence item", content)
            if (indent1 != indent || !isSequence(content)) break
            start = content
        }
        return YamlArray.owned(result)
    }

    private fun readMapping(indent: Int, first: Int, depth: Int): YamlObject {
        val explicit = linkedMapOf<String, YamlElement>()
        val merged = linkedMapOf<String, YamlElement>()
        var mergedOnce = false
        var start = first
        while (true) {
            val line = lines[lineIndex]
            checkIndent(line)
            val colon = mappingColon(start, line.end)
            if (colon < 0) fail("Expected a mapping key followed by ':'", start)
            val key = readBlockKey(start, colon, depth + 1)
            val merge = config.mergeKeys && source.substring(start, colon).trimEnd() == "<<"
            if ((!merge && explicit.containsKey(key)) || (merge && mergedOnce)) {
                if (!config.allowDuplicateKeys) fail("Duplicate mapping key '$key'", start)
            }
            val value = readValue(spaces(colon + 1, line.end), indent, depth + 1, indentlessSequence = true)
            if (merge) {
                mergeInto(merged, value, start)
                mergedOnce = true
            } else explicit[key] = value
            skipTrivia()
            if (lineIndex == lines.size || isDocumentBoundary(lines[lineIndex])) break
            (val end, val content, val indent1 = indent) = lines[lineIndex]
            if (indent1 > indent) fail("Unexpected indentation after a mapping value", content)
            if (indent1 != indent || mappingColon(content, end) < 0) break
            start = content
        }
        if (merged.isEmpty()) return YamlObject.owned(explicit)
        merged.putAll(explicit)
        return YamlObject.owned(merged)
    }

    private fun readBlockKey(start: Int, end: Int, depth: Int): String {
        checkNode(depth, start)
        val token = source.substring(start, end).trimEnd()
        if (token.isEmpty()) return ""
        if ((token[0] == '?' && (token.length == 1 || token[1].isWhitespace())) || token[0] == '[' || token[0] == '{') {
            fail("Complex and explicit mapping keys are not supported", start)
        }
        if (token[0] == '\'' || token[0] == '"' || token[0] == '!' || token[0] == '&' || token[0] == '*') {
            val reader = FlowReader(start, lines[lineIndex].indent)
            val value = reader.node(depth, key = true)
            reader.skipSpaces()
            if (reader.position != end) fail("Invalid mapping key", reader.position)
            return keyContent(value, start)
        }
        validatePlainStart(start)
        if (token.contains(" #")) fail("Comments cannot split a mapping key", start)
        return YamlScalarKind.resolveScalar(token).content
    }

    private fun keyContent(value: YamlElement, start: Int): String =
        (value as? YamlPrimitive)?.content ?: fail("Only scalar mapping keys are supported", start)

    private fun mergeInto(target: MutableMap<String, YamlElement>, value: YamlElement, start: Int) {
        fun merge(mapping: YamlObject) {
            for ([key, child] in mapping) if (!target.containsKey(key)) target[key] = child
        }
        when (value) {
            is YamlObject -> merge(value)
            is YamlArray -> for (child in value) merge(child as? YamlObject
                ?: fail("A merge sequence must contain mappings", start))
            else -> fail("A merge value must be a mapping or a sequence of mappings", start)
        }
    }

    private fun readBlockScalar(start: Int, parentIndent: Int): YamlPrimitive {
        val header = lines[lineIndex]
        val folded = source[start] == '>'
        var chomp = ' '
        var indicator = 0
        var p = start + 1
        while (p < header.end && source[p] != ' ' && source[p] != '\t' && source[p] != '#') {
            when (val c = source[p]) {
                '+', '-' -> {
                    if (chomp != ' ') fail("Duplicate chomping indicator", p)
                    chomp = c
                }
                in '1'..'9' -> {
                    if (indicator != 0) fail("Duplicate indentation indicator", p)
                    indicator = c - '0'
                }
                else -> fail("Invalid block scalar header", p)
            }
            p++
        }
        if (p < header.end && source[p] == '#') fail("Separate a block scalar comment with whitespace", p)
        p = spaces(p, header.end)
        if (p < header.end && source[p] != '#') fail("Unexpected content in block scalar header", p)
        lineIndex++
        val base = parentIndent.coerceAtLeast(0)
        var contentIndent = if (indicator == 0) -1 else base + indicator
        if (contentIndent < 0) {
            var probe = lineIndex
            var leadingIndent = 0
            while (probe < lines.size && isBlank(lines[probe])) {
                leadingIndent = maxOf(leadingIndent, lines[probe].indent)
                probe++
            }
            contentIndent = if (probe < lines.size && lines[probe].indent > parentIndent && !isDocumentBoundary(lines[probe])) {
                lines[probe].indent
            } else maxOf(base + 1, leadingIndent)
            if (leadingIndent > contentIndent) fail("A leading blank line is too indented", lines[lineIndex].start)
        }
        val parts: MutableList<BlockPart> = []
        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            if (isDocumentBoundary(line)) break
            val blank = isBlank(line)
            if (line.indent < contentIndent && charAt(line.content) == '\t') fail("Tabs cannot be used for indentation", line.content)
            if (!blank && line.indent < contentIndent) {
                if (charAt(line.content) == '#') break
                if (line.indent > parentIndent) fail("Block scalar content is less indented than its header specifies", line.content)
                break
            }
            val textStart = (line.start + contentIndent).coerceAtMost(line.end)
            // A whitespace-only final line also constitutes an empty scalar line at EOF.
            parts.add(BlockPart(textStart, line.end, line.next > line.end || (blank && line.end > line.start),
                textStart < line.end && (source[textStart] == ' ' || source[textStart] == '\t')))
            lineIndex++
        }
        val text = StringBuilder()
        var previousText: BlockPart? = null
        for (i in parts.indices) {
            val part = parts[i]
            val empty = part.start == part.end
            text.append(source, part.start, part.end)
            if (part.hasBreak) {
                val next = parts.getOrNull(i + 1)
                when {
                    !folded || next == null -> text.append('\n')
                    !empty && next.start != next.end && !part.moreIndented && !next.moreIndented -> text.append(' ')
                    empty && next.start != next.end && previousText != null &&
                        !previousText.moreIndented && !next.moreIndented -> Unit
                    else -> text.append('\n')
                }
            }
            if (!empty) previousText = part
        }
        if (chomp != '+') {
            val originalLength = text.length
            while (text.isNotEmpty() && text.last() == '\n') text.setLength(text.length - 1)
            if (chomp != '-' && text.isNotEmpty() && originalLength > text.length) text.append('\n')
        }
        return YamlPrimitive(text.toString())
    }

    private inner class FlowReader(var position: Int, private val parentIndent: Int) {
        private val originLine = lineOf(position)
        fun properties(flow: Boolean): Properties {
            var tag: String? = null
            var anchor: String? = null
            while (true) {
                if (flow) skipFlowSpace() else skipSpaces()
                when (charAt(position)) {
                    '!' -> {
                        if (tag != null) fail("A node cannot have two tags", position)
                        tag = readTag()
                    }
                    '&' -> {
                        if (anchor != null) fail("A node cannot have two anchors", position)
                        anchor = readName()
                    }
                    else -> return Properties(tag, anchor)
                }
            }
        }

        fun node(depth: Int, key: Boolean = false): YamlElement {
            skipFlowSpace()
            checkNode(depth, position)
            val start = position
            val props = properties(flow = true)
            beginAnchor(props.anchor, start)
            if (charAt(position) == '*' && (props.tag != null || props.anchor != null)) {
                fail("An alias cannot have tags or anchors", position)
            }
            val value = body(depth, flow = true, key = key, forceString = props.tag == "str")
            return finishProperties(value, props, start)
        }

        fun body(depth: Int, flow: Boolean, key: Boolean = false, forceString: Boolean = false): YamlElement = when (charAt(position)) {
            '[' -> readArray(depth)
            '{' -> readObject(depth)
            '\'', '"' -> YamlPrimitive(readQuoted())
            '*' -> {
                val start = position
                val name = readName()
                if (++aliases > config.maxAliases) fail("Alias limit exceeded", start)
                if (name in pendingAnchors) fail("Recursive aliases are not supported", start)
                anchors[name] ?: fail("Unknown or forward alias '$name'", start)
            }
            ']', '}', ',' -> if (flow) { if (forceString) YamlPrimitive("") else YamlNull }
                else fail("Unexpected flow delimiter", position)
            ':' -> if (flow && key && isSeparation(charAt(position + 1))) {
                if (forceString) YamlPrimitive("") else YamlNull
            } else {
                val text = if (flow) readPlainFlow() else readPlainBlock()
                if (forceString) YamlPrimitive(text) else YamlScalarKind.resolveScalar(text)
            }
            null -> fail("Expected a YAML value", position)
            else -> {
                val text = if (flow) readPlainFlow() else readPlainBlock()
                if (forceString) YamlPrimitive(text) else YamlScalarKind.resolveScalar(text)
            }
        }

        private fun readArray(depth: Int): YamlArray {
            position++
            val values: MutableList<YamlElement> = []
            skipFlowSpace()
            if (charAt(position) == ']') { position++; return YamlArray.owned(values) }
            while (true) {
                if (charAt(position) == ',' || charAt(position) == ']' || position >= source.length) {
                    fail("Expected a flow sequence item", position)
                }
                val start = position
                var value = node(depth + 1)
                skipFlowSpace()
                // YAML's useful compact pair notation: [name: value, other: value].
                if (charAt(position) == ':') {
                    if (lineOf(start) != lineOf(position)) fail("A flow sequence pair key must fit on one line", start)
                    val key = keyContent(value, start)
                    position++
                    skipFlowSpace()
                    val child = if (charAt(position) == ',' || charAt(position) == ']') YamlNull else node(depth + 2)
                    value = YamlObject.owned(linkedMapOf(key to child))
                    skipFlowSpace()
                }
                values.add(value)
                when (charAt(position)) {
                    ']' -> { position++; return YamlArray.owned(values) }
                    ',' -> {
                        position++
                        skipFlowSpace()
                        if (charAt(position) == ']') { position++; return YamlArray.owned(values) }
                    }
                    else -> fail("Expected ',' or ']' in flow sequence", position)
                }
            }
        }

        private fun readObject(depth: Int): YamlObject {
            position++
            val values = linkedMapOf<String, YamlElement>()
            val merged = linkedMapOf<String, YamlElement>()
            var mergedOnce = false
            skipFlowSpace()
            if (charAt(position) == '}') { position++; return YamlObject.owned(values) }
            while (true) {
                val start = position
                if ((charAt(start) == '?' && isSeparation(charAt(start + 1))) || charAt(start) == '[' || charAt(start) == '{') {
                    fail("Complex and explicit mapping keys are not supported", start)
                }
                if (charAt(start) == ',' || charAt(start) == '}' || start >= source.length) fail("Expected a mapping key", start)
                val key = if (charAt(position) == ':') "" else keyContent(node(depth + 1, key = true), start)
                val merge = config.mergeKeys && source.substring(start, position).trimEnd() == "<<"
                skipFlowSpace()
                val value = if (charAt(position) == ':') {
                    position++
                    skipFlowSpace()
                    if (charAt(position) == ',' || charAt(position) == '}') YamlNull else node(depth + 1)
                } else YamlNull
                if ((!merge && values.containsKey(key)) || (merge && mergedOnce)) {
                    if (!config.allowDuplicateKeys) fail("Duplicate mapping key '$key'", start)
                }
                if (merge) { mergeInto(merged, value, start); mergedOnce = true } else values[key] = value
                skipFlowSpace()
                when (charAt(position)) {
                    '}' -> {
                        position++
                        if (merged.isEmpty()) return YamlObject.owned(values)
                        merged.putAll(values)
                        return YamlObject.owned(merged)
                    }
                    ',' -> {
                        position++
                        skipFlowSpace()
                        if (charAt(position) == '}') {
                            position++
                            merged.putAll(values)
                            return YamlObject.owned(merged)
                        }
                    }
                    else -> fail("Expected ',' or '}' in flow mapping", position)
                }
            }
        }

        private fun readPlainFlow(): String {
            val start = position
            validatePlainStart(start, flow = true)
            while (position < source.length) {
                val c = source[position]
                if (c == '[' || c == ']' || c == '{' || c == '}' || c == ',') break
                if (c == ':' && (isSeparation(charAt(position + 1)) || charAt(position + 1) == ',' ||
                    charAt(position + 1) == ']' || charAt(position + 1) == '}')) break
                if (c == '#' && (position == start || isSeparation(source[position - 1]))) break
                if (c == '\n' || c == '\r') break
                position++
            }
            if (position == start) fail("Expected a plain scalar", start)
            var text = source.substring(start, position).trimEnd()
            if (charAt(position) == '\n' || charAt(position) == '\r') {
                val builder = StringBuilder(text)
                while (charAt(position) == '\n' || charAt(position) == '\r') {
                    val before = position
                    val comment = skipFlowSpace()
                    val c = charAt(position)
                    if (comment || c == null || c == ',' || c == ']' || c == '}' || c == ':') break
                    val continuation = position
                    while (position < source.length && source[position] !in "\r\n[]{}," &&
                        !(source[position] == ':' && isSeparation(charAt(position + 1))) &&
                        !(source[position] == '#' && isSeparation(charAt(position - 1)))) position++
                    val breaks = countBreaks(before, continuation)
                    if (breaks > 1) repeat(breaks - 1) { builder.append('\n') } else builder.append(' ')
                    builder.append(source.substring(continuation, position).trimEnd())
                }
                text = builder.toString()
            }
            return text
        }

        private fun readPlainBlock(): String {
            val start = position
            validatePlainStart(start)
            var lastIndex = lineOf(start)
            var end = commentEnd(start, lines[lastIndex].end)
            if (mappingColon(start, end) >= 0) fail("A mapping cannot be used as an inline block value", start)
            var textEnd = trimEnd(start, end)
            var builder: StringBuilder? = null
            var lastStart = start
            while (end == lines[lastIndex].end && lastIndex + 1 < lines.size) {
                var nextIndex = lastIndex + 1
                var blankLines = 0
                while (nextIndex < lines.size && isBlank(lines[nextIndex])) { nextIndex++; blankLines++ }
                if (nextIndex == lines.size) break
                val next = lines[nextIndex]
                if (next.indent <= parentIndent || isDocumentBoundary(next) || charAt(next.content) == '#') break
                val continuationStart = spaces(next.content, next.end)
                if (mappingColon(continuationStart, next.end) >= 0) {
                    fail("Unexpected mapping inside a plain scalar", continuationStart)
                }
                if (builder == null) builder = StringBuilder().append(source, start, textEnd)
                if (blankLines == 0) builder.append(' ') else repeat(blankLines) { builder.append('\n') }
                lastStart = continuationStart
                lastIndex = nextIndex
                end = commentEnd(continuationStart, next.end)
                textEnd = trimEnd(continuationStart, end)
                builder.append(source, continuationStart, textEnd)
            }
            // Trailing blank lines belong to the surrounding block, not the scalar.
            position = if (builder == null) commentEnd(start, lines[lineOf(start)].end)
                else commentEnd(lastStart, lines[lineOf(lastStart)].end)
            return builder?.toString() ?: source.substring(start, textEnd)
        }

        private fun readQuoted(): String {
            val start = position
            val quote = source[position++]
            val text = StringBuilder()
            var preservedLength = 0
            while (position < source.length) {
                when (val c = source[position++]) {
                    quote -> {
                        if (quote == '\'' && charAt(position) == '\'') { text.append('\''); position++ } else return text.toString()
                    }
                    '\\' if quote == '"' -> {
                        if (position == source.length) fail("Unterminated escape", position)
                        val escape = source[position++]
                        when (escape) {
                            '0' -> text.append('\u0000')
                            'a' -> text.append('\u0007')
                            'b' -> text.append('\b')
                            't', '\t' -> text.append('\t')
                            'n' -> text.append('\n')
                            'v' -> text.append('\u000b')
                            'f' -> text.append('\u000c')
                            'r' -> text.append('\r')
                            'e' -> text.append('\u001b')
                            ' ', '"', '/', '\\' -> text.append(escape)
                            'N' -> text.append('\u0085')
                            '_' -> text.append('\u00a0')
                            'L' -> text.append('\u2028')
                            'P' -> text.append('\u2029')
                            'x' -> appendCodePoint(text, readHex(2))
                            'u' -> appendCodePoint(text, readHex(4))
                            'U' -> appendCodePoint(text, readHex(8))
                            '\n', '\r' -> {
                                if (escape == '\r' && charAt(position) == '\n') position++
                                skipSpaces()
                                while (charAt(position) == '\r' || charAt(position) == '\n') {
                                    consumeBreak()
                                    skipSpaces()
                                    text.append('\n')
                                }
                                checkContinuationIndent()
                            }

                            else -> fail("Unknown escape '\\$escape'", position - 2)
                        }
                        if (escape != '\n' && escape != '\r') preservedLength = text.length
                    }
                    '\n', '\r' -> {
                        while (text.length > preservedLength && (text.last() == ' ' || text.last() == '\t')) text.setLength(text.length - 1)
                        if (c == '\r' && charAt(position) == '\n') position++
                        skipSpaces()
                        var emptyLines = 0
                        while (charAt(position) == '\n' || charAt(position) == '\r') {
                            consumeBreak()
                            skipSpaces()
                            emptyLines++
                        }
                        checkContinuationIndent()
                        if (position < source.length && isDocumentBoundary(lines[lineOf(position)])) {
                            fail("A document marker cannot occur inside a quoted scalar", position)
                        }
                        if (emptyLines == 0) text.append(' ') else repeat(emptyLines) { text.append('\n') }
                    }
                    else -> text.append(c)
                }
            }
            fail("Unterminated quoted scalar", start)
        }

        private fun readHex(count: Int): Int {
            val start = position
            var value = 0L
            repeat(count) {
                val digit = charAt(position)?.digitToIntOrNull(16) ?: fail("Invalid Unicode escape", position)
                position++
                value = (value shl 4) or digit.toLong()
            }
            if (value > 0x10ffff || value in 0xd800..0xdfff) fail("Invalid Unicode code point", start)
            return value.toInt()
        }

        private fun readName(): String {
            val start = position++
            val nameStart = position
            while (position < source.length && !isSeparation(source[position]) && source[position] !in "[]{},") position++
            if (position == nameStart) fail("Expected an anchor or alias name", start)
            return source.substring(nameStart, position)
        }

        private fun readTag(): String {
            val start = position++
            val text = if (charAt(position) == '<') {
                position++
                val nameStart = position
                while (position < source.length && source[position] != '>' && !isSeparation(source[position])) position++
                if (charAt(position) != '>') fail("Unterminated verbatim tag", start)
                val name = source.substring(nameStart, position++)
                if (!name.startsWith("tag:yaml.org,2002:")) fail("Custom tags are not supported", start)
                name.removePrefix("tag:yaml.org,2002:")
            } else {
                while (position < source.length && !isSeparation(source[position]) && source[position] !in "[]{},") position++
                val name = source.substring(start, position)
                if (name == "!") "str" else if (name.startsWith("!!")) name.substring(2)
                else fail("Custom tags are not supported", start)
            }
            when (text) {
                "str", "null", "bool", "int", "float", "map", "seq" -> Unit
                else -> fail("Unsupported tag '$text'", start)
            }
            return text
        }

        fun skipSpaces() { position = spaces(position, source.length) }

        private fun skipFlowSpace(): Boolean {
            var comment = false
            while (position < source.length) {
                when (source[position]) {
                    ' ', '\t', '\n', '\r' -> position++
                    '#' -> {
                        if (position > 0 && !isSeparation(source[position - 1])) fail("Separate a comment with whitespace", position)
                        comment = true
                        while (position < source.length && source[position] != '\n' && source[position] != '\r') position++
                    }
                    else -> {
                        val line = lines[lineOf(position)]
                        if (position == line.content && isDocumentBoundary(line)) fail("Unclosed flow collection before a document marker", position)
                        checkContinuationIndent()
                        return comment
                    }
                }
            }
            return comment
        }

        private fun checkContinuationIndent() {
            if (position >= source.length) return
            val index = lineOf(position)
            if (index > originLine && lines[index].indent <= parentIndent) {
                fail("A multiline value must be indented beyond its parent", position)
            }
        }

        private fun consumeBreak() {
            if (charAt(position++) == '\r' && charAt(position) == '\n') position++
        }

        fun finishLine() {
            val index = lineOf(position.coerceAtMost(source.length))
            (val start, val end) = lines[index]
            position = spaces(position, end)
            if (position < end && source[position] != '#') fail("Unexpected content after YAML value", position)
            if (position in (start + 1) ..< end && !isSeparation(source[position - 1])) {
                fail("Separate a comment from its value with whitespace", position)
            }
            lineIndex = index + 1
        }
    }

    private fun beginAnchor(name: String?, start: Int) {
        if (name != null && !pendingAnchors.add(name)) fail("Recursive anchors are not supported", start)
    }

    private fun finishProperties(value: YamlElement, properties: Properties, start: Int): YamlElement {
        val tagged = when (properties.tag) {
            null -> value
            "str" -> if (value is YamlPrimitive) YamlPrimitive(if (value is YamlNull) "null" else value.content) else fail("!!str requires a scalar", start)
            "null" -> if (value is YamlPrimitive && YamlScalarKind.fromString(value.content) == YamlScalarKind.Null) YamlNull
                else fail("Invalid !!null value", start)
            "bool" -> if (value is YamlPrimitive && YamlScalarKind.fromString(value.content) == YamlScalarKind.Boolean) YamlScalarKind.resolveScalar(value.content)
                else fail("Invalid !!bool value", start)
            "int" -> if (value is YamlPrimitive && YamlScalarKind.fromString(value.content) == YamlScalarKind.Integer) YamlLiteral(value.content, YamlScalarKind.Integer)
                else fail("Invalid !!int value", start)
            "float" -> {
                if (value is YamlPrimitive && (YamlScalarKind.fromString(value.content) == YamlScalarKind.Integer || YamlScalarKind.fromString(value.content) == YamlScalarKind.Float)) {
                    if (value.content.contains('x') || value.content.contains('o')) {
                        YamlPrimitive(YamlScalarKind.scalarDouble(value.content) ?: fail("Invalid !!float value", start))
                    } else {
                        val content = if (YamlScalarKind.fromString(value.content) == YamlScalarKind.Integer) value.content + ".0" else value.content
                        YamlLiteral(content, YamlScalarKind.Float)
                    }
                } else fail("Invalid !!float value", start)
            }
            "map" -> value as? YamlObject ?: fail("!!map requires a mapping", start)
            "seq" -> value as? YamlArray ?: fail("!!seq requires a sequence", start)
            else -> fail("Unsupported tag", start)
        }
        properties.anchor?.let { anchors[it] = tagged; pendingAnchors.remove(it) }
        return tagged
    }

    private fun indexLines(): List<Line> {
        val result: MutableList<Line> = []
        var start = 0
        while (start < source.length) {
            var end = start
            while (end < source.length && source[end] != '\r' && source[end] != '\n') end++
            var next = end
            if (next < source.length && source[next++] == '\r' && charAt(next) == '\n') next++
            val origin = if (start == 0 && charAt(start) == '\ufeff') start + 1 else start
            var content = origin
            while (content < end && source[content] == ' ') content++
            result.add(Line(start, end, next, content, content - origin))
            start = next
        }
        // Also supplies a location for an empty input and for errors exactly at EOF.
        if (result.isEmpty() || result.last().next > result.last().end) {
            result.add(Line(source.length, source.length, source.length, source.length, 0))
        }
        for (i in source.indices) {
            val c = source[i]
            if ((c < ' ' && c != '\t' && c != '\r' && c != '\n') || c in '\u007f'..'\u0084' || c in '\u0086'..'\u009f' || c == '\ufffe' || c == '\uffff') {
                // Initialization cannot call fail(), which uses the not-yet-assigned lines property.
                var index = 0
                while (index + 1 < result.size && result[index + 1].start <= i) index++
                throw YamlParseException("Invalid control character", index + 1, i - result[index].start + 1, i)
            }
        }
        return result
    }

    private fun skipTrivia() {
        while (lineIndex < lines.size) {
            (val end, val content) = lines[lineIndex]
            val start = spaces(content, end)
            if (start != end && source[start] != '#') return
            lineIndex++
        }
    }

    private fun checkIndent(line: Line) {
        if (charAt(line.content) == '\t') fail("Tabs cannot be used for indentation", line.content)
    }

    private fun checkNode(depth: Int, start: Int) {
        if (depth > config.maxDepth) fail("Maximum YAML nesting depth exceeded", start)
        if (++nodes > config.maxNodes) fail("YAML node limit exceeded", start)
    }

    private fun mappingColon(start: Int, end: Int): Int {
        var quote = ' '
        var nesting = 0
        var collectionStart = start
        while (collectionStart < end && (source[collectionStart] == '!' || source[collectionStart] == '&')) {
            while (collectionStart < end && !isSeparation(source[collectionStart])) collectionStart++
            collectionStart = spaces(collectionStart, end)
        }
        var i = start
        while (i < end) {
            val c = source[i]
            if (quote == '"') {
                if (c == '\\') i++ else if (c == '"') quote = ' '
            } else if (quote == '\'') {
                if (c == '\'') {
                    if (i + 1 < end && source[i + 1] == '\'') i++ else quote = ' '
                }
            } else when (c) {
                '"', '\'' -> if (i == start || isSeparation(charAt(i - 1)) ||
                    charAt(i - 1) == '[' || charAt(i - 1) == '{' || charAt(i - 1) == ',' || charAt(i - 1) == ':') quote = c
                '[', '{' -> if (nesting > 0 || i == collectionStart) nesting++
                ']', '}' -> if (nesting > 0) nesting--
                '#' -> if (i == start || isSeparation(charAt(i - 1))) return -1
                ':' -> if (nesting == 0 && (i + 1 == end || isSeparation(source[i + 1]))) return i
            }
            i++
        }
        return -1
    }

    private fun validatePlainStart(start: Int, flow: Boolean = false) {
        val c = charAt(start) ?: fail("Expected a scalar", start)
        if (c in "@`%|>!&*[]{},#\"'" || ((c == '?' || c == ':' || c == '-') &&
                (isSeparation(charAt(start + 1)) || (flow && charAt(start + 1)?.let { it in "[]{}," } == true)))) {
            fail("Invalid plain scalar start; quote this value or use a supported collection", start)
        }
    }

    private fun commentEnd(start: Int, end: Int): Int {
        var i = start
        while (i < end) {
            if (source[i] == '#' && (i == start || isSeparation(source[i - 1]))) return i
            i++
        }
        return end
    }

    private fun isSequence(start: Int): Boolean = charAt(start) == '-' && isSeparation(charAt(start + 1))
    private fun isBlank(line: Line): Boolean = spaces(line.content, line.end) == line.end
    private fun isDocumentBoundary(line: Line): Boolean = isMarker(line, "---") || isMarker(line, "...")
    private fun isMarker(line: Line, marker: String): Boolean = line.indent == 0 &&
        source.regionMatches(line.content, marker, 0, marker.length) &&
        (line.content + marker.length == line.end || isSeparation(charAt(line.content + marker.length)))
    private fun isSeparation(c: Char?): Boolean = c == null || c == ' ' || c == '\t' || c == '\r' || c == '\n'
    private fun charAt(offset: Int): Char? = source.getOrNull(offset)
    private fun spaces(start: Int, end: Int): Int {
        var i = start
        while (i < end && (source[i] == ' ' || source[i] == '\t')) i++
        return i
    }
    private fun trimEnd(start: Int, end: Int): Int {
        var i = end
        while (i > start && (source[i - 1] == ' ' || source[i - 1] == '\t')) i--
        return i
    }
    private fun countBreaks(start: Int, end: Int): Int {
        var count = 0
        var i = start
        while (i < end) {
            if (source[i] == '\r') { count++; if (i + 1 < end && source[i + 1] == '\n') i++ }
            else if (source[i] == '\n') count++
            i++
        }
        return count
    }
    private fun appendCodePoint(text: StringBuilder, value: Int) {
        if (value <= 0xffff) text.append(value.toChar())
        else {
            text.append(((value - 0x10000) / 0x400 + 0xd800).toChar())
            text.append(((value - 0x10000) % 0x400 + 0xdc00).toChar())
        }
    }
    private fun lineOf(offset: Int): Int {
        if (lineContains(locatedLine, offset)) return locatedLine
        if (lineContains(lineIndex, offset)) { locatedLine = lineIndex; return locatedLine }
        if (lineContains(locatedLine + 1, offset)) { locatedLine++; return locatedLine }
        var low = 0
        var high = lines.lastIndex
        while (low < high) {
            val middle = (low + high + 1) ushr 1
            if (lines[middle].start <= offset) low = middle else high = middle - 1
        }
        locatedLine = low
        return low
    }
    private fun lineContains(index: Int, offset: Int): Boolean = index in lines.indices &&
        offset >= lines[index].start && (offset < lines[index].next || offset == lines[index].end)
    private fun currentOffset(): Int = lines.getOrNull(lineIndex)?.content ?: source.length
    private fun fail(message: String, offset: Int): Nothing {
        val index = lineOf(offset)
        throw YamlParseException(message, index + 1, offset - lines[index].start + 1, offset)
    }
}