package love.yinlin.foundation.parser.markdown

import androidx.annotation.IntRange

class MarkdownList private constructor(
    override val children: List<MarkdownListItem>,
    val ordered: Boolean,
    @IntRange(0, 999999999) val start: Int,
    val tight: Boolean,
    @Suppress("unused") owned: Unit,
) : MarkdownBlock(), List<MarkdownListItem> by children {
    private companion object {
        private fun requiresLooseMarkdownItem(item: MarkdownListItem): Boolean {
            for (index in 1 until item.size) if (item[index - 1] is MarkdownParagraph) {
                val next = item[index]
                if (next is MarkdownParagraph || next is MarkdownList && next.ordered && next.start != 1) return true
            }
            return false
        }
    }

    constructor(content: List<MarkdownListItem>, ordered: Boolean = false, start: Int = 1, tight: Boolean = true) :
        this(content.toList(), ordered, start, tight && content.none { requiresLooseMarkdownItem(it) }, Unit)

    init {
        require(children.isNotEmpty()) { "A list must have at least one item" }
        require(start in 0 .. 999999999) { "List start must have at most nine digits" }
        require(ordered || start == 1) { "Unordered lists cannot have a start number" }
    }

    override fun equals(other: Any?): Boolean = other is MarkdownList && children == other.children && ordered == other.ordered && start == other.start && tight == other.tight
    override fun hashCode(): Int = 31 * (31 * (31 * children.hashCode() + ordered.hashCode()) + start) + tight.hashCode()
}