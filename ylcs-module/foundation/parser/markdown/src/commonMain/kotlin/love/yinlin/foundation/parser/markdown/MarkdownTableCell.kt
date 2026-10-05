package love.yinlin.foundation.parser.markdown

class MarkdownTableCell(content: List<MarkdownInline>) : MarkdownElement() {
    override val children: List<MarkdownInline> = normalizeMarkdownInlines(content)
    constructor(text: String) : this([MarkdownText(text)])
    init { require(children.none { it is MarkdownSoftBreak || it is MarkdownHardBreak }) { "A table cell must be a single line" } }
    override fun equals(other: Any?): Boolean = other is MarkdownTableCell && children == other.children
    override fun hashCode(): Int = children.hashCode()
}
