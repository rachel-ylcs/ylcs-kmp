package love.yinlin.foundation.parser.markdown

class MarkdownBlockQuote private constructor(
    override val children: List<MarkdownBlock>,
    @Suppress("unused") owned: Unit,
) : MarkdownBlock(), List<MarkdownBlock> by children {
    constructor(content: List<MarkdownBlock>) : this(content.toList(), Unit)

    override fun equals(other: Any?): Boolean = other is MarkdownBlockQuote && children == other.children
    override fun hashCode(): Int = children.hashCode()
}
