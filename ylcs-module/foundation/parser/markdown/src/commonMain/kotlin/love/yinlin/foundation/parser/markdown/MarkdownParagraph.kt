package love.yinlin.foundation.parser.markdown

class MarkdownParagraph(content: List<MarkdownInline>) : MarkdownBlock() {
    override val children: List<MarkdownInline> = normalizeMarkdownInlines(content)
    constructor(text: String) : this([MarkdownText(text)])

    init {
        require(children.isNotEmpty()) { "A paragraph must have inline content" }
    }

    operator fun get(index: Int): MarkdownInline = children[index]
    override fun equals(other: Any?): Boolean = other is MarkdownParagraph && children == other.children
    override fun hashCode(): Int = children.hashCode()
}
