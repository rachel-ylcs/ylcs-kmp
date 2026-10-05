package love.yinlin.foundation.parser.markdown

import androidx.annotation.IntRange

class MarkdownHeading(@IntRange(1, 6) val level: Int, content: List<MarkdownInline>) : MarkdownBlock() {
    override val children: List<MarkdownInline> = normalizeMarkdownInlines(content)
    constructor(level: Int, text: String) : this(level, [MarkdownText(text)])

    init {
        require(level in 1..6) { "Heading level must be between 1 and 6" }
        require(children.none { it is MarkdownSoftBreak || it is MarkdownHardBreak }) { "A heading must be a single line" }
    }

    operator fun get(index: Int): MarkdownInline = children[index]
    override fun equals(other: Any?): Boolean = other is MarkdownHeading && level == other.level && children == other.children
    override fun hashCode(): Int = 31 * level + children.hashCode()
}