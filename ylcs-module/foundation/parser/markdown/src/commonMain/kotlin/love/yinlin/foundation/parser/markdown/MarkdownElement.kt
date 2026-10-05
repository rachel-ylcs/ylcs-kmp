package love.yinlin.foundation.parser.markdown

sealed class MarkdownElement {
    open val children: List<MarkdownElement> = []

    final override fun toString(): String = Markdown.encodeToString(this)

    internal companion object {
        fun normalizeMarkdownInlines(content: List<MarkdownInline>): List<MarkdownInline> {
            val result = ArrayList<MarkdownInline>(minOf(content.size, 64))
            var pending: MarkdownText? = null
            var text: StringBuilder? = null

            for (node in content) {
                if (node is MarkdownText) {
                    if (node.content.isNotEmpty()) {
                        if (pending == null) pending = node
                        else {
                            if (text == null) text = StringBuilder(pending.content)
                            text.append(node.content)
                        }
                    }
                } else {
                    val builder = text
                    if (builder != null) result += MarkdownText(builder.toString())
                    else pending?.let { result += it }
                    pending = null
                    text = null
                    result += node
                }
            }
            val builder = text
            if (builder != null) result += MarkdownText(builder.toString())
            else pending?.let { result += it }
            return result
        }
    }
}