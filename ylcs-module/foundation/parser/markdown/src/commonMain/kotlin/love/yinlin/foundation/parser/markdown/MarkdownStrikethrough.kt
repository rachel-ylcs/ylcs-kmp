package love.yinlin.foundation.parser.markdown

class MarkdownStrikethrough(content: List<MarkdownInline>) : MarkdownInlineContainer(content) {
    constructor(text: String) : this([MarkdownText(text)])
    override fun equals(other: Any?): Boolean = other is MarkdownStrikethrough && children == other.children
    override fun hashCode(): Int = children.hashCode()
}