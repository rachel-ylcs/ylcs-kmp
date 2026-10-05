package love.yinlin.foundation.parser.markdown

class MarkdownEmphasis(content: List<MarkdownInline>) : MarkdownInlineContainer(content) {
    constructor(text: String) : this([MarkdownText(text)])
    override fun equals(other: Any?): Boolean = other is MarkdownEmphasis && children == other.children
    override fun hashCode(): Int = children.hashCode()
}