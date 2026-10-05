package love.yinlin.foundation.parser.markdown

internal class MarkdownParser(source: String, private val config: MarkdownConfiguration) {
    private val context = MarkdownParseContext(source, config)
    private val references = HashMap<String, MarkdownLinkTarget>()

    private data class Line(val text: String, val offset: Int)

    private sealed class Draft {
        class Ready(val value: MarkdownBlock, val offset: Int) : Draft()
        class Inline(val text: String, val level: Int, val offset: Int) : Draft()
        class Quote(val blocks: List<Draft>, val offset: Int) : Draft()
        class Items(val items: List<Item>, val marker: Marker, val tight: Boolean, val offset: Int) : Draft()
        class Table(val cells: List<List<String>>, val align: List<MarkdownAlignment>, val offset: Int) : Draft()
    }

    private data class Item(val blocks: List<Draft>, val checked: Boolean?, val offset: Int)

    private data class Marker(val ordered: Boolean, val number: Int, val char: Char, val indent: Int, val content: Int, val width: Int)

    private data class Fence(val char: Char, val length: Int, val indent: Int, val info: String)

    fun parse(): MarkdownDocument {
        if (context.source.length > config.maxSourceLength) context.fail("Markdown source length limit exceeded", 0)
        context.node(0, 0)
        val drafts = blocks(lines(), 1)
        return MarkdownDocument(drafts.map { resolve(it, 1) })
    }

    private fun lines(): List<Line> {
        val source = context.source
        val result: MutableList<Line> = []
        var start = 0
        var i = 0
        while (i < source.length) {
            if (source[i] == '\r' || source[i] == '\n') {
                result += Line(source.substring(start, i).replace('\u0000', '\ufffd'), start)
                if (source[i] == '\r' && source.getOrNull(i + 1) == '\n') i++
                start = i + 1
            }
            i++
        }
        if (start < source.length) result += Line(source.substring(start).replace('\u0000', '\ufffd'), start)
        return result
    }

    private fun draftOffset(draft: Draft): Int = when (draft) {
        is Draft.Ready -> draft.offset
        is Draft.Inline -> draft.offset
        is Draft.Quote -> draft.offset
        is Draft.Items -> draft.offset
        is Draft.Table -> draft.offset
    }

    private fun inline(text: String, parent: Int, offset: Int): List<MarkdownInline> = MarkdownInlineParser(text, context, references, parent, offset).parse()

    private fun resolve(draft: Draft, depth: Int): MarkdownBlock {
        val offset = draftOffset(draft)
        context.node(depth, offset)

        return when (draft) {
            is Draft.Ready -> draft.value
            is Draft.Inline -> {
                if (draft.level == 0) MarkdownParagraph(inline(draft.text, depth, offset))
                else MarkdownHeading(draft.level, inline(draft.text, depth, offset))
            }
            is Draft.Quote -> MarkdownBlockQuote(draft.blocks.map { resolve(it, depth + 1) })
            is Draft.Items -> MarkdownList(draft.items.map { (val blocks, val checked, val offset1 = offset) ->
                context.node(depth + 1, offset1)
                MarkdownListItem(blocks.map { resolve(it, depth + 2) }, checked)
            }, draft.marker.ordered, if (draft.marker.ordered) draft.marker.number else 1, draft.tight)
            is Draft.Table -> {
                val rows = draft.cells.map { row ->
                    context.node(depth + 1, offset)
                    MarkdownTableRow(row.map { cell ->
                        context.node(depth + 2, offset)
                        MarkdownTableCell(inline(cell, depth + 2, offset))
                    })
                }
                MarkdownTable(rows.first(), draft.align, rows.drop(1))
            }
        }
    }

    private fun blocks(lines: List<Line>, depth: Int): List<Draft> {
        val result: MutableList<Draft> = []
        var i = 0
        while (i < lines.size) {
            (val text, val offset) = lines[i]
            if (blank(text)) {
                i++
                continue
            }
            context.depth(depth, offset)
            val fence = fence(text)
            if (fence != null) {
                val code = StringBuilder()
                i++
                while (i < lines.size && !closesFence(lines[i].text, fence)) {
                    code.append(strip(lines[i].text, fence.indent)).append('\n')
                    i++
                }
                if (i < lines.size) i++
                result += Draft.Ready(MarkdownCodeBlock(code.toString(), fence.info), offset)
                continue
            }
            val heading = heading(text)
            if (heading != null) {
                result += Draft.Inline(heading.second, heading.first, offset)
                i++
                continue
            }
            if (thematic(text)) {
                result += Draft.Ready(MarkdownThematicBreak, offset)
                i++
                continue
            }
            if (quoteStart(text) >= 0) {
                val inner: MutableList<Line> = []
                var lazy = false
                while (i < lines.size) {
                    val current = lines[i]
                    val start = quoteStart(current.text)
                    if (start >= 0) {
                        val body = current.text.substring(start)
                        inner += Line(body, current.offset + start)
                        lazy = body.isNotBlank() && !interrupts(body)
                    } else if (lazy && !blank(current.text) && !interrupts(current.text) && setext(current.text) == 0) {
                        inner += current
                    } else break
                    i++
                }
                result += Draft.Quote(blocks(inner, depth + 1), offset)
                continue
            }
            val marker = marker(text)
            if (marker != null) {
                val [item, index] = list(lines, i, marker, depth)
                result += item
                i = index
                continue
            }
            if (indent(text) >= 4) {
                val code = StringBuilder()
                while (i < lines.size) {
                    val current = lines[i].text
                    if (indent(current) >= 4) code.append(strip(current, 4)).append('\n')
                    else if (blank(current)) code.append('\n')
                    else break
                    i++
                }
                while (code.endsWith("\n\n")) code.setLength(code.length - 1)
                result += Draft.Ready(MarkdownCodeBlock(code.toString()), offset)
                continue
            }
            val htmlEnd = htmlEnd(text)
            if (htmlEnd != null) {
                val html = StringBuilder()
                do {
                    val current = lines[i++].text
                    html.append(current).append('\n')
                    if (htmlEnd.isNotEmpty() && current.contains(htmlEnd, ignoreCase = true)) break
                } while (i < lines.size && (htmlEnd.isNotEmpty() || !blank(lines[i].text)))
                result += Draft.Ready(MarkdownHtmlBlock(html.toString().trimEnd('\n')), offset)
                continue
            }
            val ref = reference(lines, i)
            if (ref > i) {
                i = ref
                continue
            }
            if (config.tables && i + 1 < lines.size && hasPipe(text)) {
                val align = tableAlignment(lines[i + 1].text)
                val head = tableCells(text)
                if (align != null && head.size == align.size) {
                    val rows: MutableList<List<String>> = [head]
                    i += 2
                    while (i < lines.size && !blank(lines[i].text) && !interrupts(lines[i].text)) {
                        val cells = tableCells(lines[i++].text)
                        rows += List(head.size) { cells.getOrElse(it) { "" } }
                    }
                    result += Draft.Table(rows, align, offset)
                    continue
                }
            }
            val paragraph = StringBuilder(text.trimStart(' ', '\t'))
            i++
            var level = 0
            while (i < lines.size && !blank(lines[i].text)) {
                val current = lines[i].text
                level = setext(current)
                if (level != 0) {
                    i++
                    break
                }
                if (interrupts(current)) break
                paragraph.append('\n').append(current.trimStart(' ', '\t'))
                i++
            }
            val value = paragraph.toString().trimEnd(' ', '\t')
            result += Draft.Inline(if (level == 0) value else value.replace('\n', ' '), level, offset)
        }
        return result
    }

    private fun list(lines: List<Line>, start: Int, first: Marker, depth: Int): Pair<Draft.Items, Int> {
        val items: MutableList<Item> = []
        var i = start
        var tight = true
        while (i < lines.size) {
            (val text, val offset1 = offset) = lines[i]
            val current = marker(text) ?: break
            if (!sameList(first, current) || thematic(text)) break
            val inner: MutableList<Line> = []
            var body = text.substring(current.content)
            var checked: Boolean? = null
            if (config.taskLists && body.length >= 3 && body[0] == '[' && body[2] == ']' &&
                body[1] in " xX" && (body.length == 3 || body[3] == ' ' || body[3] == '\t')) {
                checked = body[1] != ' '
                body = body.substring(3).trimStart(' ', '\t')
            }
            inner += Line(body, offset1 + current.content)
            var lazy = body.isNotBlank() && !interrupts(body)
            var pendingBlank = false
            val afterBlank = HashSet<Int>()
            i++
            while (i < lines.size) {
                val next = lines[i]
                if (blank(next.text)) {
                    inner += Line(strip(next.text, current.width), next.offset + minOf(current.width, next.text.length))
                    pendingBlank = true
                    lazy = false
                    i++
                    continue
                }
                val nextMarker = marker(next.text)
                if (nextMarker != null && nextMarker.indent < current.width) break
                if (indent(next.text) >= current.width) {
                    val stripped = strip(next.text, current.width)
                    val offset = next.offset + minOf(current.width, next.text.length)
                    if (pendingBlank) afterBlank += offset
                    inner += Line(stripped, offset)
                    lazy = !interrupts(stripped)
                    pendingBlank = false
                    i++
                } else if (lazy && !interrupts(next.text) && setext(next.text) == 0) {
                    inner += next
                    i++
                } else break
            }
            val nextMarker = lines.getOrNull(i)?.let { marker(it.text) }
            if (pendingBlank && nextMarker != null && sameList(first, nextMarker)) tight = false
            context.depth(depth + 1, offset1)
            val children = blocks(inner, depth + 2)
            if (afterBlank.isNotEmpty() && children.any { draftOffset(it) in afterBlank }) tight = false
            items += Item(children, checked, offset1)
        }
        return Draft.Items(items, first, tight, lines[start].offset) to i
    }

    private fun sameList(a: Marker, b: Marker): Boolean = a.ordered == b.ordered && a.char == b.char

    private fun reference(lines: List<Line>, index: Int): Int {
        val text = lines[index].text
        if (indent(text) > 3) return index
        var i = text.indexOfFirst { it != ' ' && it != '\t' }
        if (text.getOrNull(i) != '[') return index
        val start = ++i
        while (i < text.length && i - start <= 999 && text[i] != ']') {
            if (text[i] == '[') return index
            if (text[i] == '\\' && i + 1 < text.length) i++
            i++
        }
        if (text.getOrNull(i) != ']' || text.getOrNull(i + 1) != ':') return index
        val label = markdownReference(text.substring(start, i))
        if (label.isEmpty()) return index
        i += 2
        while (i < text.length && markdownSpace(text[i])) i++
        var targetText = text
        var consumed = index + 1
        if (i == text.length && index + 1 < lines.size) {
            targetText = lines[index + 1].text.trim()
            i = 0
            consumed++
        }
        (val value, val end) = scanMarkdownDestination(targetText, i) ?: return index
        if (end == i) return index
        i = end
        val beforeSpace = i
        while (i < targetText.length && markdownSpace(targetText[i])) i++
        var title: String? = null
        if (i < targetText.length) {
            if (i == beforeSpace) return index
            (val value1 = value, val end1 = end) = scanMarkdownTitle(targetText, i) ?: return index
            if (targetText.substring(end1).isNotBlank()) return index
            title = value1
        } else if (consumed < lines.size) {
            val next = lines[consumed].text.trim()
            val scan = scanMarkdownTitle(next, 0)
            if (scan != null && next.substring(scan.end).isBlank()) {
                title = scan.value
                consumed++
            }
        }
        val _ = references.getOrPut(label) { MarkdownLinkTarget(value, title) }
        return consumed
    }

    private fun interrupts(text: String): Boolean {
        if (heading(text) != null || fence(text) != null || thematic(text) || quoteStart(text) >= 0) return true
        val list = marker(text)
        if (list != null && list.content < text.length && (!list.ordered || list.number == 1)) return true
        return htmlEnd(text) != null
    }

    private fun blank(text: String): Boolean = text.all { it == ' ' || it == '\t' }

    private fun indent(text: String): Int {
        var columns = 0
        for (c in text) when (c) {
            ' ' -> columns++
            '\t' -> columns += 4 - columns % 4
            else -> return columns
        }
        return columns
    }

    private fun strip(text: String, columns: Int): String {
        var width = 0
        var i = 0
        while (i < text.length && width < columns) {
            if (text[i] == ' ') width++
            else if (text[i] == '\t') width += 4 - width % 4
            else break
            i++
        }
        return " ".repeat(maxOf(0, width - columns)) + text.substring(i)
    }

    private fun prefix(text: String): Int {
        if (indent(text) > 3) return -1
        return text.indexOfFirst { it != ' ' && it != '\t' }.let { if (it < 0) text.length else it }
    }

    private fun heading(text: String): Pair<Int, String>? {
        var i = prefix(text)
        if (i < 0 || text.getOrNull(i) != '#') return null
        val start = i
        while (text.getOrNull(i) == '#') i++
        val level = i - start
        if (level > 6 || (i < text.length && text[i] != ' ' && text[i] != '\t')) return null
        var body = text.substring(i).trim(' ', '\t')
        var end = body.length
        while (end > 0 && body[end - 1] == '#') end--
        if (end < body.length && (end == 0 || body[end - 1] == ' ' || body[end - 1] == '\t')) body = body.substring(0, end).trimEnd(' ', '\t')
        return level to body
    }

    private fun setext(text: String): Int {
        val start = prefix(text)
        if (start < 0) return 0
        val body = text.substring(start).trimEnd(' ', '\t')
        if (body.isEmpty()) return 0
        return if (body.all { it == '=' }) 1 else if (body.all { it == '-' }) 2 else 0
    }

    private fun thematic(text: String): Boolean {
        val start = prefix(text)
        if (start < 0) return false
        val char = text.getOrNull(start) ?: return false
        if (char !in "*-_") return false
        var count = 0
        for (i in start until text.length) {
            if (text[i] == char) count++ else if (text[i] != ' ' && text[i] != '\t') return false
        }
        return count >= 3
    }

    private fun fence(text: String): Fence? {
        val start = prefix(text)
        if (start < 0) return null
        val char = text.getOrNull(start) ?: return null
        if (char != '`' && char != '~') return null
        var i = start
        while (text.getOrNull(i) == char) i++
        if (i - start < 3) return null
        val info = text.substring(i).trim()
        if (char == '`' && '`' in info) return null
        return Fence(char, i - start, indent(text), decodeMarkdownText(info))
    }

    private fun closesFence(text: String, fence: Fence): Boolean {
        val start = prefix(text)
        if (start < 0) return false
        var i = start
        while (text.getOrNull(i) == fence.char) i++
        return i - start >= fence.length && text.substring(i).all { it == ' ' || it == '\t' }
    }

    private fun quoteStart(text: String): Int {
        val start = prefix(text)
        if (start < 0 || text.getOrNull(start) != '>') return -1
        return start + if (text.getOrNull(start + 1) == ' ' || text.getOrNull(start + 1) == '\t') 2 else 1
    }

    private fun marker(text: String): Marker? {
        val start = prefix(text)
        if (start < 0 || start == text.length) return null
        var i = start
        val ordered = text[i] in '0'..'9'
        var number = 1
        if (ordered) {
            while (text.getOrNull(i) in '0'..'9') i++
            if (i - start > 9 || text.getOrNull(i) !in ['.', ')']) return null
            number = text.substring(start, i).toInt()
        } else if (text[i] !in "-+*") return null
        val char = text[i++]
        if (i < text.length && text[i] != ' ' && text[i] != '\t') return null
        val after = i
        var columns = indent(text) + i - start
        val before = columns
        while (i < text.length && (text[i] == ' ' || text[i] == '\t')) {
            columns += if (text[i] == '\t') 4 - columns % 4 else 1
            i++
        }
        val padding = columns - before
        val content = if (padding > 4) after + 1 else i
        val width = before + if (padding in 1..4 && i < text.length) padding else 1
        return Marker(ordered, number, char, indent(text), content, width)
    }

    private fun hasPipe(text: String): Boolean {
        var i = 0
        while (i < text.length) {
            if (text[i] == '\\' && i + 1 < text.length) i++
            else if (text[i] == '|') return true
            i++
        }
        return false
    }

    private fun tableCells(text: String): List<String> {
        val cells: MutableList<String> = []
        var start = 0
        var i = 0
        var lastSeparator = -1
        while (i < text.length) {
            if (text[i] == '\\' && i + 1 < text.length) i++
            else if (text[i] == '|') {
                cells += text.substring(start, i).trim()
                start = i + 1
                lastSeparator = i
            }
            i++
        }
        cells += text.substring(start).trim()
        if (text.trimStart().startsWith('|')) cells.removeAt(0)
        if (lastSeparator == text.trimEnd().lastIndex && cells.isNotEmpty()) cells.removeAt(cells.lastIndex)
        return cells.map { cell ->
            buildString(cell.length) {
                var index = 0
                while (index < cell.length) {
                    if (cell[index] == '\\' && index + 1 < cell.length) {
                        if (cell[index + 1] != '|') append('\\')
                        append(cell[++index])
                    } else append(cell[index])
                    index++
                }
            }
        }
    }

    private fun tableAlignment(text: String): List<MarkdownAlignment>? {
        if (indent(text) > 3) return null
        val cells = tableCells(text)
        if (cells.isEmpty()) return null
        val align: MutableList<MarkdownAlignment> = []
        for (cell in cells) {
            val dashes = cell.trim(':')
            if (dashes.isEmpty() || dashes.any { it != '-' }) return null
            align += when {
                cell.startsWith(':') && cell.endsWith(':') -> MarkdownAlignment.Center
                cell.startsWith(':') -> MarkdownAlignment.Left
                cell.endsWith(':') -> MarkdownAlignment.Right
                else -> MarkdownAlignment.None
            }
        }
        return align
    }

    private fun htmlEnd(text: String): String? {
        val start = prefix(text)
        if (start < 0 || text.getOrNull(start) != '<') return null
        val body = text.substring(start)
        if (body.startsWith("<!--")) return "-->"
        if (body.startsWith("<?")) return "?>"
        if (body.startsWith("<![CDATA[")) return "]]>"
        if (body.startsWith("<!") && body.getOrNull(2)?.isUpperCase() == true) return ">"
        var i = if (body.getOrNull(1) == '/') 2 else 1
        val tagStart = i
        while (body.getOrNull(i)?.isLetterOrDigit() == true) i++
        if (i == tagStart || body.getOrNull(i) !in [' ', '\t', '>', '/']) return null
        val tag = body.substring(tagStart, i).lowercase()
        if (tag in ["script", "style", "pre", "textarea"]) return "</$tag>"
        return if (tag in HTMLBlockTags) "" else null
    }

    private companion object {
        val HTMLBlockTags: Set<String> = [
            "address", "article", "aside", "base", "blockquote", "body", "caption", "center", "col", "colgroup",
            "dd", "details", "dialog", "dir", "div", "dl", "dt", "fieldset", "figcaption", "figure", "footer", "form",
            "frame", "frameset", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hr", "html", "iframe",
            "legend", "li", "link", "main", "menu", "nav", "ol", "p", "param", "section", "summary", "table", "tbody",
            "td", "tfoot", "th", "thead", "title", "tr", "track", "ul"
        ]
    }
}
