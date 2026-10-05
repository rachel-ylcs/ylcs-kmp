package love.yinlin.foundation.parser.markdown

internal class MarkdownInlineParser(
    private val source: String,
    private val context: MarkdownParseContext,
    private val references: Map<String, MarkdownLinkTarget>,
    private val parentDepth: Int,
    private val offset: Int,
) {
    private class Node(var value: MarkdownInline, var height: Int = 1, val plain: Boolean = false) {
        var literal: StringBuilder? = null
        var previous: Node? = null
        var next: Node? = null

        fun read(): MarkdownInline = literal?.let { MarkdownText(it.toString()) } ?: value
    }

    private class Delimiter(val node: Node, val char: Char, var length: Int, val open: Boolean, val close: Boolean, val id: Int) {
        var previous: Delimiter? = null
        var next: Delimiter? = null
    }

    private class Bracket(val node: Node, val image: Boolean, val start: Int, val bottom: Int, val previous: Bracket?) {
        var active: Boolean = true
    }

    private val head = Node(MarkdownText(""))
    private var tail = head
    private var firstDelimiter: Delimiter? = null
    private var lastDelimiter: Delimiter? = null
    private var delimiterId = 0
    private var bracket: Bracket? = null
    private val text = StringBuilder()
    private val ticks = HashMap<Int, ArrayDeque<Int>>()

    fun parse(): List<MarkdownInline> {
        indexTicks()
        var i = 0
        while (i < source.length) {
            val c = source[i]
            when {
                c == '\\' -> {
                    val next = source.getOrNull(i + 1)
                    if (next == '\n') {
                        flush()
                        append(Node(MarkdownHardBreak))
                        i += 2
                        i = skipLineIndent(i)
                        continue
                    }
                    if (next != null && markdownPunctuation(next)) {
                        text.append(next)
                        i += 2
                        continue
                    }
                    text.append(c)
                }
                c == '\n' -> {
                    var spaces = 0
                    while (text.isNotEmpty() && (text.last() == ' ' || text.last() == '\t')) {
                        if (text.last() == ' ') spaces++ else spaces = 0
                        text.setLength(text.length - 1)
                    }
                    flush()
                    append(Node(if (spaces >= 2) MarkdownHardBreak else MarkdownSoftBreak))
                    i = skipLineIndent(i + 1)
                    continue
                }
                c == '`' -> {
                    var end = i + 1
                    while (source.getOrNull(end) == '`') end++
                    val queue = ticks[end - i]!!
                    while (queue.isNotEmpty() && queue.first() <= i) queue.removeFirst()
                    if (queue.isNotEmpty()) {
                        val close = queue.removeFirst()
                        var code = source.substring(end, close).replace('\n', ' ')
                        if (code.startsWith(' ') && code.endsWith(' ') && code.any { it != ' ' }) code = code.substring(1, code.lastIndex)
                        flush()
                        append(Node(MarkdownCode(code)))
                        i = close + end - i
                    }
                    else {
                        text.append(source, i, end)
                        i = end
                    }
                    continue
                }
                c == '*' || c == '_' || (c == '~' && context.config.strikethrough) -> {
                    var end = i + 1
                    while (source.getOrNull(end) == c) end++
                    val length = end - i
                    if (c == '~' && length != 2) {
                        text.append(source, i, end)
                        i = end
                        continue
                    }
                    val before = source.getOrNull(i - 1)
                    val after = source.getOrNull(end)
                    val beforeSpace = before == null || before.isWhitespace()
                    val afterSpace = after == null || after.isWhitespace()
                    val beforePunct = before != null && punctuation(before)
                    val afterPunct = after != null && punctuation(after)
                    val left = !afterSpace && (!afterPunct || beforeSpace || beforePunct)
                    val right = !beforeSpace && (!beforePunct || afterSpace || afterPunct)
                    val open = left && (c != '_' || !right || beforePunct)
                    val close = right && (c != '_' || !left || afterPunct)
                    flush()
                    val node = append(Node(MarkdownText(source.substring(i, end))))
                    if (open || close) push(Delimiter(node, c, length, open, close, ++delimiterId))
                    i = end
                    continue
                }
                c == '[' || (c == '!' && source.getOrNull(i + 1) == '[') -> {
                    flush()
                    val image = c == '!'
                    val node = append(Node(MarkdownText(if (image) "![" else "[")))
                    bracket = Bracket(node, image, i + if (image) 2 else 1, delimiterId, bracket)
                    i += if (image) 2 else 1
                    continue
                }
                c == ']' -> {
                    flush()
                    val open = bracket
                    if (open == null || !open.active) {
                        if (open != null) bracket = open.previous
                        text.append(']')
                        i++
                        continue
                    }
                    val target = linkTarget(i + 1, open)
                    if (target != null) {
                        processDelimiters(open.bottom)
                        val children = collect(open.node.next, null)
                        val height = 1 + maxHeight(open.node.next, null)
                        context.depth(parentDepth + height, offset + i)
                        open.node.value = if (open.image) MarkdownImage(target.first.destination, children, target.first.title)
                            else MarkdownLink(target.first.destination, children, target.first.title)
                        open.node.height = height
                        open.node.next = null
                        tail = open.node
                        bracket = open.previous
                        if (!open.image) {
                            var earlier = bracket
                            while (earlier != null) {
                                if (!earlier.image) {
                                    if (!earlier.active) break
                                    earlier.active = false
                                }
                                earlier = earlier.previous
                            }
                        }
                        i = target.second
                        continue
                    }
                    bracket = open.previous
                    text.append(']')
                }
                c == '&' -> {
                    val entity = scanMarkdownEntity(source, i)
                    if (entity != null) {
                        text.append(entity.value)
                        i = entity.end
                        continue
                    }
                    text.append('&')
                }
                c == '<' -> {
                    val angle = angle(i)
                    if (angle != null) {
                        flush()
                        append(Node(angle.first))
                        i = angle.second
                        continue
                    }
                    text.append('<')
                }
                else -> text.append(c)
            }
            i++
        }
        flush()
        processDelimiters(0)
        val result = MarkdownElement.normalizeMarkdownInlines(collect(head.next, null))
        countNodes(result)
        return result
    }

    private fun countNodes(nodes: List<MarkdownInline>) {
        val stack: MutableList<Pair<Iterator<MarkdownInline>, Int>> = [nodes.iterator() to parentDepth + 1]
        while (stack.isNotEmpty()) {
            val [iterator, depth] = stack.last()
            if (!iterator.hasNext()) {
                stack.removeAt(stack.lastIndex)
                continue
            }
            val node = iterator.next()
            context.node(depth, offset)
            if (node is MarkdownInlineContainer && node.children.isNotEmpty()) stack += node.children.iterator() to depth + 1
        }
    }

    private fun indexTicks() {
        var i = 0
        while (i < source.length) {
            if (source[i] == '`') {
                val start = i++
                while (source.getOrNull(i) == '`') i++
                ticks.getOrPut(i - start) { ArrayDeque() }.addLast(start)
            } else i++
        }
    }

    private fun skipLineIndent(start: Int): Int {
        var i = start
        while (source.getOrNull(i) == ' ' || source.getOrNull(i) == '\t') i++
        return i
    }

    private fun punctuation(c: Char): Boolean = !c.isLetterOrDigit() && !c.isWhitespace()

    private fun append(node: Node): Node {
        tail.next = node
        node.previous = tail
        tail = node
        return node
    }

    private fun flush() {
        if (text.isEmpty()) return
        if (tail.plain) {
            if (tail.literal == null) tail.literal = StringBuilder((tail.value as MarkdownText).content)
            tail.literal!!.append(text)
        }
        else append(Node(MarkdownText(text.toString()), plain = true))
        text.setLength(0)
    }

    private fun push(delimiter: Delimiter) {
        delimiter.previous = lastDelimiter
        lastDelimiter?.next = delimiter
        if (firstDelimiter == null) firstDelimiter = delimiter
        lastDelimiter = delimiter
    }

    private fun remove(delimiter: Delimiter) {
        delimiter.previous?.next = delimiter.next
        delimiter.next?.previous = delimiter.previous
        if (firstDelimiter === delimiter) firstDelimiter = delimiter.next
        if (lastDelimiter === delimiter) lastDelimiter = delimiter.previous
    }

    private fun unlink(node: Node) {
        node.previous?.next = node.next
        node.next?.previous = node.previous
        if (tail === node) tail = node.previous ?: head
    }

    private fun collect(start: Node?, end: Node?): List<MarkdownInline> {
        val result: MutableList<MarkdownInline> = []
        var node = start
        while (node != null && node !== end) {
            result += node.read()
            node = node.next
        }
        return result
    }

    private fun maxHeight(start: Node?, end: Node?): Int {
        var node = start
        var height = 0
        while (node != null && node !== end) {
            height = maxOf(height, node.height)
            node = node.next
        }
        return height
    }

    private fun processDelimiters(bottom: Int) {
        val bounds = IntArray(18) { bottom }
        var closer = firstDelimiter
        while (closer != null && closer.id <= bottom) closer = closer.next
        while (closer != null) {
            if (!closer.close) {
                closer = closer.next
                continue
            }
            val kind = when (closer.char) {
                '*' -> 0
                '_' -> 1
                else -> 2
            }
            val key = kind * 6 + (if (closer.open) 3 else 0) + closer.length % 3
            var opener = closer.previous
            while (opener != null && opener.id > bounds[key]) {
                val odd = closer.char != '~' && (opener.close || closer.open) &&
                    (opener.length + closer.length) % 3 == 0 && (opener.length % 3 != 0 || closer.length % 3 != 0)
                if (opener.char == closer.char && opener.open && !odd) break
                opener = opener.previous
            }
            if (opener == null || opener.id <= bounds[key]) {
                bounds[key] = closer.previous?.id ?: bottom
                val next = closer.next
                if (!closer.open) remove(closer)
                closer = next
                continue
            }
            val used = if (opener.length >= 2 && closer.length >= 2) 2 else 1
            val content = collect(opener.node.next, closer.node)
            val height = 1 + maxHeight(opener.node.next, closer.node)
            context.depth(parentDepth + height, offset)
            val value = when {
                closer.char == '~' -> MarkdownStrikethrough(content)
                used == 2 -> MarkdownStrong(content)
                else -> MarkdownEmphasis(content)
            }
            val wrapped = Node(value, height)
            opener.node.next = wrapped
            wrapped.previous = opener.node
            wrapped.next = closer.node
            closer.node.previous = wrapped
            var inside = opener.next
            while (inside != null && inside !== closer) {
                val next = inside.next
                remove(inside)
                inside = next
            }
            opener.length -= used
            closer.length -= used
            opener.node.value = MarkdownText(opener.char.toString().repeat(opener.length))
            closer.node.value = MarkdownText(closer.char.toString().repeat(closer.length))
            if (opener.length == 0) {
                unlink(opener.node)
                remove(opener)
            }
            if (closer.length == 0) {
                val next = closer.next
                unlink(closer.node)
                remove(closer)
                closer = next
            }
        }
        var remaining = lastDelimiter
        while (remaining != null && remaining.id > bottom) {
            val previous = remaining.previous
            remove(remaining)
            remaining = previous
        }
    }

    private fun linkTarget(start: Int, bracket: Bracket): Pair<MarkdownLinkTarget, Int>? {
        if (source.getOrNull(start) == '(') {
            var i = start + 1
            while (i < source.length && markdownSpace(source[i])) i++
            val destination = scanMarkdownDestination(source, i)
            if (destination != null) {
                i = destination.end
                val beforeSpace = i
                while (i < source.length && markdownSpace(source[i])) i++
                var title: String? = null
                val titleScan = if (i > beforeSpace) scanMarkdownTitle(source, i) else null
                if (titleScan != null) {
                    title = titleScan.value
                    i = titleScan.end
                    while (i < source.length && markdownSpace(source[i])) i++
                }
                if (source.getOrNull(i) == ')') return MarkdownLinkTarget(destination.value, title) to i + 1
            }
        }
        var end = start
        var label: String? = null
        if (source.getOrNull(start) == '[') {
            var i = start + 1
            while (i < source.length && i - start <= 1000 && source[i] != ']') {
                if (source[i] == '[') break
                if (source[i] == '\\' && i + 1 < source.length) i++
                i++
            }
            if (source.getOrNull(i) == ']') {
                label = source.substring(start + 1, i)
                end = i + 1
            }
            else return null
        }
        if (label.isNullOrEmpty()) {
            val close = start - 1
            if (close - bracket.start > 999) return null
            label = source.substring(bracket.start, close)
        }
        val target = references[markdownReference(label)] ?: return null
        return target to end
    }

    private fun angle(start: Int): Pair<MarkdownInline, Int>? {
        if (source.startsWith("<!--", start)) {
            val close = source.indexOf("-->", start + 4)
            if (close >= 0) return MarkdownHtmlInline(source.substring(start, close + 3)) to close + 3
            return null
        }
        var end = start + 1
        var quote: Char? = null
        while (end < source.length) {
            val c = source[end]
            if (c == '<' && quote == null) return null
            if (c == '>' && quote == null) break
            if (c == '\n') return null
            if (c == '"' || c == '\'') { if (quote == null) quote = c else if (quote == c) quote = null }
            end++
        }
        if (end == source.length) return null
        val value = source.substring(start + 1, end)
        val colon = value.indexOf(':')
        if (colon in 2..32 && value[0].isLetter() && value.substring(0, colon).all { it.isLetterOrDigit() || it in "+.-" } &&
            value.none { it.isWhitespace() || it == '<' || it == '>' }) {
            return MarkdownLink(value, value) to end + 1
        }
        val at = value.indexOf('@')
        if (at > 0 && value.indexOf('@', at + 1) == -1 && value.substring(0, at).all { it.isLetterOrDigit() || it in ".!#$%&'*+/=?^_`{|}~-" } &&
            value.substring(at + 1).split('.').all { it.isNotEmpty() && it.first() != '-' && it.last() != '-' && it.all { c -> c.isLetterOrDigit() || c == '-' } }) {
            return MarkdownLink("mailto:$value", value) to end + 1
        }
        val tag = value.removePrefix("/")
        if (tag.isNotEmpty() && tag[0].isLetter() && tag.takeWhile { it.isLetterOrDigit() || it == '-' }.isNotEmpty()) {
            val nameEnd = tag.indexOfFirst { !it.isLetterOrDigit() && it != '-' }.let { if (it < 0) tag.length else it }
            if (nameEnd == tag.length || tag[nameEnd].isWhitespace() || tag[nameEnd] == '/')
                return MarkdownHtmlInline(source.substring(start, end + 1)) to end + 1
        }
        return null
    }
}
