package love.yinlin.foundation.parser.markdown

internal class MarkdownWriter(private val config: MarkdownConfiguration) {
    private val output = StringBuilder()
    private var nodes = 0

    fun write(element: MarkdownElement): String {
        when (element) {
            is MarkdownDocument -> {
                visit(0)
                blocks(element.children, "", 1)
            }
            is MarkdownBlock -> block(element, "", "", 0)
            is MarkdownInline -> inline(element, 0, false, '\u0000')
            is MarkdownListItem -> {
                visit(0)
                blocks(element.children, "", 1)
            }
            is MarkdownTableRow -> row(element, "", 0)
            is MarkdownTableCell -> {
                visit(0)
                inlines(element.children, 1, true)
            }
        }
        return output.toString()
    }

    private fun visit(depth: Int) {
        require(depth <= config.maxDepth) { "Maximum Markdown nesting depth exceeded while writing" }
        require(++nodes <= config.maxNodes) { "Markdown node limit exceeded while writing" }
    }

    private fun blocks(content: List<MarkdownBlock>, prefix: String, depth: Int, first: String = prefix, tight: Boolean = false) {
        var previousAlternate = false
        for (i in content.indices) {
            if (i > 0 && (!tight || content[i - 1] is MarkdownParagraph && content[i] is MarkdownParagraph)) output.append(prefix).append('\n')
            val previous = content.getOrNull(i - 1)
            val alternate = previous is MarkdownList && content[i] is MarkdownList &&
                previous.ordered == (content[i] as MarkdownList).ordered && !previousAlternate
            block(content[i], prefix, if (i == 0) first else prefix, depth, alternate)
            previousAlternate = alternate
        }
    }

    private fun block(value: MarkdownBlock, prefix: String, first: String, depth: Int, alternate: Boolean = false) {
        visit(depth)
        when (value) {
            is MarkdownParagraph -> {
                output.append(first)
                inlines(value.children, depth + 1, false, continuation = prefix)
                output.append('\n')
            }
            is MarkdownHeading -> {
                output.append(first).append("#".repeat(value.level)).append(' ')
                inlines(value.children, depth + 1, false)
                output.append('\n')
            }
            is MarkdownThematicBreak -> output.append(first).append("***\n")
            is MarkdownCodeBlock -> {
                val char = if ('`' in value.info) '~' else '`'
                val fence = char.toString().repeat(maxOf(3, longestRun(value.content, char) + 1))
                output.append(first).append(fence)
                if (value.info.isNotEmpty()) {
                    output.append(' ')
                    escaped(value.info)
                }
                output.append('\n')
                prefixed(value.content, prefix)
                output.append(prefix).append(fence).append('\n')
            }
            is MarkdownBlockQuote -> {
                if (value.isEmpty()) output.append(first).append(">\n")
                else blocks(value.children, "$prefix> ", depth + 1, "$first> ")
            }
            is MarkdownList -> {
                require(config.taskLists || value.none { it.checked != null }) { "Task lists are disabled" }
                for (i in value.indices) {
                    if (i > 0 && !value.tight) output.append(prefix).append('\n')
                    val item = value[i]
                    visit(depth + 1)
                    val marker = if (value.ordered) {
                        val number = (value.start.toLong() + i).let { if (it <= 999_999_999) it else 1 }
                        "$number${if (alternate) ')' else '.'} "
                    } else if (alternate) "+ " else "- "
                    val lead = (if (i == 0) first else prefix) + marker
                    val continued = prefix + " ".repeat(marker.length)
                    val task = when (item.checked) {
                        true -> "[x] "
                        false -> "[ ] "
                        null -> ""
                    }
                    if (item.isEmpty()) output.append(lead).append(task).append('\n')
                    else if (!value.tight && value.size == 1 && item.size == 1) {
                        output.append(lead).append(task).append('\n').append(continued).append('\n')
                        blocks(item.children, continued, depth + 2, tight = false)
                    } else blocks(item.children, continued, depth + 2, lead + task, value.tight)
                }
            }
            is MarkdownTable -> {
                require(config.tables) { "Tables are disabled" }
                row(value.header, first, depth + 1)
                output.append(prefix).append('|')
                for (alignment in value.alignments) output.append(' ').append(when (alignment) {
                    MarkdownAlignment.None -> "---"
                    MarkdownAlignment.Left -> ":---"
                    MarkdownAlignment.Center -> ":---:"
                    MarkdownAlignment.Right -> "---:"
                }).append(" |")
                output.append('\n')
                for (row in value.rows) row(row, prefix, depth + 1)
            }
            is MarkdownHtmlBlock -> {
                output.append(first)
                val raw = value.content.replace("\r\n", "\n").replace('\r', '\n')
                var start = 0
                for (i in raw.indices) if (raw[i] == '\n') {
                    output.append(raw, start, i + 1)
                    if (i < raw.lastIndex) output.append(prefix)
                    start = i + 1
                }
                output.append(raw, start, raw.length)
                if (!raw.endsWith('\n')) output.append('\n')
            }
        }
    }

    private fun row(row: MarkdownTableRow, first: String, depth: Int) {
        visit(depth)
        output.append(first).append('|')
        for (cell in row) {
            visit(depth + 1)
            output.append(' ')
            inlines(cell.children, depth + 2, true)
            output.append(" |")
        }
        output.append('\n')
    }

    private fun inlines(content: List<MarkdownInline>, depth: Int, table: Boolean, marker: Char = '\u0000', continuation: String = "") {
        var previousMarker: Char? = null
        for (i in content.indices) {
            val value = content[i]
            if (value is MarkdownText) {
                visit(depth)
                text(value.content, content.getOrNull(i - 1) is MarkdownInlineContainer, content.getOrNull(i + 1) is MarkdownInlineContainer)
            } else if (value is MarkdownSoftBreak || value is MarkdownHardBreak) {
                visit(depth)
                require(!table) { "Table cells cannot contain line breaks" }
                if (value is MarkdownHardBreak) output.append('\\')
                output.append('\n').append(continuation)
            } else if (value is MarkdownEmphasis || value is MarkdownStrong) {
                val selected = if ((previousMarker ?: marker) == '*') '_' else '*'
                inline(value, depth, table, marker, continuation, selected)
                previousMarker = selected
                continue
            } else inline(value, depth, table, marker, continuation)
            previousMarker = null
        }
    }

    private fun inline(value: MarkdownInline, depth: Int, table: Boolean, parentMarker: Char, continuation: String = "", selectedMarker: Char? = null) {
        visit(depth)
        when (value) {
            is MarkdownText -> text(value.content, boundaryStart = false, boundaryEnd = false)
            is MarkdownCode -> {
                require(value.content.isNotEmpty()) { "Empty inline code has no Markdown representation" }
                val fence = "`".repeat(longestRun(value.content, '`') + 1)
                val padded = value.content.startsWith('`') || value.content.endsWith('`') ||
                    value.content.startsWith(' ') && value.content.endsWith(' ') && value.content.any { it != ' ' }
                output.append(fence)
                if (padded) output.append(' ')
                for (c in value.content) {
                    if (table && c == '|') output.append('\\')
                    output.append(c)
                }
                if (padded) output.append(' ')
                output.append(fence)
            }
            is MarkdownEmphasis, is MarkdownStrong, is MarkdownStrikethrough -> {
                require(value.children.isNotEmpty()) { "Empty emphasis has no Markdown representation" }
                if (value is MarkdownStrikethrough) require(config.strikethrough) { "Strikethrough is disabled" }
                val marker = if (value is MarkdownStrikethrough) '~' else selectedMarker ?: if (parentMarker == '*') '_' else '*'
                val fence = marker.toString().repeat(if (value is MarkdownEmphasis) 1 else 2)
                output.append(fence)
                inlines(value.children, depth + 1, table, marker, continuation)
                output.append(fence)
            }
            is MarkdownLink -> {
                require(value.elements<MarkdownLink>().drop(1).none()) { "Markdown links cannot contain links" }
                output.append('[')
                inlines(value.children, depth + 1, table, continuation = continuation)
                output.append("](")
                target(value.destination, value.title, table)
                output.append(')')
            }
            is MarkdownImage -> {
                output.append("![")
                inlines(value.children, depth + 1, table, continuation = continuation)
                output.append("](")
                target(value.destination, value.title, table)
                output.append(')')
            }
            is MarkdownSoftBreak, is MarkdownHardBreak -> {
                require(!table) { "Table cells cannot contain line breaks" }
                if (value is MarkdownHardBreak) output.append('\\')
                output.append('\n').append(continuation)
            }
            is MarkdownHtmlInline -> {
                require(!table || '\n' !in value.content && '\r' !in value.content) { "Table HTML must be a single line" }
                if (table) {
                    for (c in value.content) {
                        if (c == '|') output.append('\\')
                        output.append(c)
                    }
                }
                else output.append(value.content)
            }
        }
    }

    private fun target(destination: String, title: String?, table: Boolean) {
        output.append('<')
        escaped(destination, table)
        output.append('>')
        if (title != null) {
            output.append(" \"")
            escaped(title, table)
            output.append('"')
        }
    }
    private fun escaped(value: String, table: Boolean = false) {
        for (c in value) when {
            c == '\n' || c == '\r' || c == '\t' -> output.append("&#").append(c.code).append(';')
            c == '\\' || c == '<' || c == '>' || c == '&' || c == '"' || table && c == '|' -> output.append('\\').append(c)
            else -> output.append(c)
        }
    }
    private fun text(value: String, boundaryStart: Boolean, boundaryEnd: Boolean) {
        for (i in value.indices) {
            val c = value[i]
            when {
                c == '\n' || c == '\r' || c == '\t' || c == ' ' && (i == 0 || i == value.lastIndex) ||
                    i == 0 && boundaryStart && c.isLetterOrDigit() || i == value.lastIndex && boundaryEnd && c.isLetterOrDigit() ->
                    output.append("&#").append(c.code).append(';')
                markdownPunctuation(c) -> output.append('\\').append(c)
                else -> output.append(c)
            }
        }
    }
    private fun longestRun(value: String, char: Char): Int {
        var max = 0
        var current = 0
        for (c in value) {
            current = if (c == char) current + 1 else 0
            max = maxOf(max, current)
        }
        return max
    }
    private fun prefixed(value: String, prefix: String) {
        var start = 0
        for (i in value.indices) if (value[i] == '\n') {
            output.append(prefix).append(value, start, i + 1)
            start = i + 1
        }
        if (start < value.length) output.append(prefix).append(value, start, value.length).append('\n')
    }
}
