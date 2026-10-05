package love.yinlin.foundation.parser.markdown

class MarkdownStrong(content: List<MarkdownInline>) : MarkdownInlineContainer(content) {
    constructor(text: String) : this([MarkdownText(text)])
    override fun equals(other: Any?): Boolean = other is MarkdownStrong && children == other.children
    override fun hashCode(): Int = children.hashCode()
}