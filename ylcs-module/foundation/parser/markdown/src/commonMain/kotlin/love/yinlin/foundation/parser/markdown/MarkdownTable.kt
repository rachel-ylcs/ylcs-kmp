package love.yinlin.foundation.parser.markdown

class MarkdownTable(
    val header: MarkdownTableRow,
    alignments: List<MarkdownAlignment> = List(header.size) { MarkdownAlignment.None },
    rows: List<MarkdownTableRow> = [],
) : MarkdownBlock() {
    val alignments: List<MarkdownAlignment> = alignments.toList()
    val rows: List<MarkdownTableRow> = rows.toList()
    override val children: List<MarkdownTableRow> = [header] + this.rows

    init {
        require(header.isNotEmpty()) { "A table must have at least one column" }
        require(this.alignments.size == header.size) { "Table alignment count must match the header" }
        require(this.rows.all { it.size == header.size }) { "Table rows must match the header width" }
    }

    override fun equals(other: Any?): Boolean = other is MarkdownTable && header == other.header && alignments == other.alignments && rows == other.rows
    override fun hashCode(): Int = 31 * (31 * header.hashCode() + alignments.hashCode()) + rows.hashCode()
}