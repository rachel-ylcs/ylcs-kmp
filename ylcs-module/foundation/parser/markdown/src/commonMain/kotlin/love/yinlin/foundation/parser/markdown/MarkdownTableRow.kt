package love.yinlin.foundation.parser.markdown

class MarkdownTableRow private constructor(
    override val children: List<MarkdownTableCell>,
    @Suppress("unused") owned: Unit,
) : MarkdownElement(), List<MarkdownTableCell> by children {
    constructor(cells: List<MarkdownTableCell>) : this(cells.toList(), Unit)
    override fun equals(other: Any?): Boolean = other is MarkdownTableRow && children == other.children
    override fun hashCode(): Int = children.hashCode()
}