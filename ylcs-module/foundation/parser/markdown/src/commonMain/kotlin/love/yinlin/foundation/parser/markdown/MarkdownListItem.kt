package love.yinlin.foundation.parser.markdown

class MarkdownListItem private constructor(
    override val children: List<MarkdownBlock>,
    val checked: Boolean?,
    @Suppress("unused") owned: Unit,
) : MarkdownElement(), List<MarkdownBlock> by children {
    constructor(content: List<MarkdownBlock>, checked: Boolean? = null) : this(content.toList(), checked, Unit)

    override fun equals(other: Any?): Boolean = other is MarkdownListItem && children == other.children && checked == other.checked
    override fun hashCode(): Int = 31 * children.hashCode() + checked.hashCode()
}
