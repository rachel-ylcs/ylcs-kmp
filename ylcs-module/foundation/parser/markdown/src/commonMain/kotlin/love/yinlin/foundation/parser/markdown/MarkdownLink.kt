package love.yinlin.foundation.parser.markdown

class MarkdownLink(val destination: String, content: List<MarkdownInline>, val title: String? = null) : MarkdownInlineContainer(content) {
    constructor(destination: String, text: String, title: String? = null) : this(destination, [MarkdownText(text)], title)
    override fun equals(other: Any?): Boolean = other is MarkdownLink && destination == other.destination && title == other.title && children == other.children
    override fun hashCode(): Int = 31 * (31 * destination.hashCode() + children.hashCode()) + title.hashCode()
}