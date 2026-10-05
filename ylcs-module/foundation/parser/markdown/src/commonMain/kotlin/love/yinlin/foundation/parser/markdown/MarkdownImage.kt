package love.yinlin.foundation.parser.markdown

class MarkdownImage(val destination: String, content: List<MarkdownInline>, val title: String? = null) : MarkdownInlineContainer(content) {
    constructor(destination: String, alt: String, title: String? = null) : this(destination, [MarkdownText(alt)], title)
    val alt: String get() = plainText
    override fun equals(other: Any?): Boolean = other is MarkdownImage && destination == other.destination && title == other.title && children == other.children
    override fun hashCode(): Int = 31 * (31 * destination.hashCode() + children.hashCode()) + title.hashCode()
}