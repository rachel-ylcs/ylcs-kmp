package love.yinlin.foundation.parser.markdown

import org.intellij.lang.annotations.Language
import kotlin.contracts.*

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class MarkdownDsl

@MarkdownDsl
class MarkdownInlineBuilder @PublishedApi internal constructor() {
    @PublishedApi
    internal val content: MutableList<MarkdownInline> = []

    fun add(value: MarkdownInline) { content += value }
    operator fun String.unaryPlus() { text(this) }

    fun text(value: String) {
        var start = 0
        var i = 0
        while (i < value.length) {
            if (value[i] == '\r' || value[i] == '\n') {
                if (i > start) add(MarkdownText(value.substring(start, i)))
                add(MarkdownSoftBreak)
                if (value[i] == '\r' && value.getOrNull(i + 1) == '\n') i++
                start = i + 1
            }
            i++
        }
        if (start < value.length) add(MarkdownText(value.substring(start)))
    }

    fun em(block: MarkdownInlineBuilder.() -> Unit) { add(MarkdownEmphasis(inlines(block))) }
    fun em(text: String) { em { +text } }
    fun strong(block: MarkdownInlineBuilder.() -> Unit) { add(MarkdownStrong(inlines(block))) }
    fun strong(text: String) { strong { +text } }
    fun del(block: MarkdownInlineBuilder.() -> Unit) { add(MarkdownStrikethrough(inlines(block))) }
    fun del(text: String) { del { +text } }
    fun code(text: String) { add(MarkdownCode(text)) }
    fun br() { add(MarkdownHardBreak) }
    fun softBreak() { add(MarkdownSoftBreak) }
    fun a(href: String, title: String? = null, block: MarkdownInlineBuilder.() -> Unit) { add(MarkdownLink(href, inlines(block), title)) }
    fun a(href: String, text: String, title: String? = null) { a(href, title) { +text } }
    fun img(src: String, alt: String = "", title: String? = null) { add(MarkdownImage(src, alt, title)) }
    fun img(src: String, title: String? = null, block: MarkdownInlineBuilder.() -> Unit) { add(MarkdownImage(src, inlines(block), title)) }
    fun html(@Language("HTML") source: String) { add(MarkdownHtmlInline(source)) }
}

@MarkdownDsl
open class MarkdownBlockBuilder @PublishedApi internal constructor() {
    @PublishedApi
    internal val content: MutableList<MarkdownBlock> = []

    fun add(value: MarkdownBlock) { content += value }
    fun p(block: MarkdownInlineBuilder.() -> Unit) { add(MarkdownParagraph(inlines(block))) }
    fun p(text: String) { p { +text } }
    fun h(level: Int, block: MarkdownInlineBuilder.() -> Unit) { add(MarkdownHeading(level, inlines(block))) }
    fun h(level: Int, text: String) { h(level) { +text } }
    fun h1(block: MarkdownInlineBuilder.() -> Unit) { h(1, block) }
    fun h1(text: String) { h(1, text) }
    fun h2(block: MarkdownInlineBuilder.() -> Unit) { h(2, block) }
    fun h2(text: String) { h(2, text) }
    fun h3(block: MarkdownInlineBuilder.() -> Unit) { h(3, block) }
    fun h3(text: String) { h(3, text) }
    fun h4(block: MarkdownInlineBuilder.() -> Unit) { h(4, block) }
    fun h4(text: String) { h(4, text) }
    fun h5(block: MarkdownInlineBuilder.() -> Unit) { h(5, block) }
    fun h5(text: String) { h(5, text) }
    fun h6(block: MarkdownInlineBuilder.() -> Unit) { h(6, block) }
    fun h6(text: String) { h(6, text) }
    fun hr() { add(MarkdownThematicBreak) }
    fun codeBlock(code: String, info: String = "") { add(MarkdownCodeBlock(code, info)) }
    fun blockquote(block: MarkdownBlockBuilder.() -> Unit) { add(MarkdownBlockQuote(MarkdownBlockBuilder().apply(block).content)) }
    fun ul(tight: Boolean = true, block: MarkdownListBuilder.() -> Unit) {
        add(MarkdownList(MarkdownListBuilder().apply(block).content, tight = tight))
    }
    fun ol(start: Int = 1, tight: Boolean = true, block: MarkdownListBuilder.() -> Unit) {
        add(MarkdownList(MarkdownListBuilder().apply(block).content, ordered = true, start = start, tight = tight))
    }
    fun table(block: MarkdownTableBuilder.() -> Unit) { add(MarkdownTableBuilder().apply(block).build()) }
    fun html(@Language("HTML") source: String) { add(MarkdownHtmlBlock(source)) }
}

@MarkdownDsl
class MarkdownListBuilder internal constructor() {
    internal val content: MutableList<MarkdownListItem> = []
    fun li(checked: Boolean? = null, block: MarkdownBlockBuilder.() -> Unit) {
        content += MarkdownListItem(MarkdownBlockBuilder().apply(block).content, checked)
    }
    fun li(text: String, checked: Boolean? = null) { li(checked) { p(text) } }
}

@MarkdownDsl
class MarkdownTableBuilder internal constructor() {
    private var header: MarkdownTableRow? = null
    private var alignments: List<MarkdownAlignment>? = null
    private val rows: MutableList<MarkdownTableRow> = []

    fun header(vararg titles: String) { header = MarkdownTableRow(titles.map { MarkdownTableCell(it) }) }
    fun header(block: MarkdownTableRowBuilder.() -> Unit) { header = row(block) }
    fun align(vararg values: MarkdownAlignment) { alignments = values.toList() }
    fun tr(vararg values: String) { rows += MarkdownTableRow(values.map { MarkdownTableCell(it) }) }
    fun tr(block: MarkdownTableRowBuilder.() -> Unit) { rows += row(block) }

    private fun row(block: MarkdownTableRowBuilder.() -> Unit): MarkdownTableRow =
        MarkdownTableRow(MarkdownTableRowBuilder().apply(block).content)

    internal fun build(): MarkdownTable {
        val head = requireNotNull(header) { "A table DSL requires a header" }
        return MarkdownTable(head, alignments ?: List(head.size) { MarkdownAlignment.None }, rows)
    }
}

@MarkdownDsl
class MarkdownTableRowBuilder internal constructor() {
    internal val content: MutableList<MarkdownTableCell> = []
    fun td(text: String) { content += MarkdownTableCell(text) }
    fun td(block: MarkdownInlineBuilder.() -> Unit) { content += MarkdownTableCell(inlines(block)) }
}

internal fun inlines(block: MarkdownInlineBuilder.() -> Unit): List<MarkdownInline> = MarkdownInlineBuilder().apply(block).content

@OptIn(ExperimentalContracts::class)
inline fun markdown(block: MarkdownBlockBuilder.() -> Unit): MarkdownDocument {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return MarkdownDocument(MarkdownBlockBuilder().apply(block).content)
}