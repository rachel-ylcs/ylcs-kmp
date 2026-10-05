package love.yinlin.foundation.parser.markdown

sealed class MarkdownInlineContainer(content: List<MarkdownInline>) : MarkdownInline() {
    final override val children: List<MarkdownInline> = normalizeMarkdownInlines(content)
    operator fun get(index: Int): MarkdownInline = children[index]
}