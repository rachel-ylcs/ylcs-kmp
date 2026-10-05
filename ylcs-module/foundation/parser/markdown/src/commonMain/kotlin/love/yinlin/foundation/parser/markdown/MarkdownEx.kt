package love.yinlin.foundation.parser.markdown

val MarkdownElement.asSequence: Sequence<MarkdownElement> get() = sequence {
    yield(this@asSequence)
    val stack: MutableList<Iterator<MarkdownElement>> = []
    stack.add(children.iterator())
    while (stack.isNotEmpty()) {
        val iterator = stack.last()
        if (!iterator.hasNext()) {
            stack.removeAt(stack.lastIndex)
            continue
        }
        val node = iterator.next()
        yield(node)
        if (node.children.isNotEmpty()) stack.add(node.children.iterator())
    }
}

inline fun <reified T : MarkdownElement> MarkdownElement.elements(): Sequence<T> = this.asSequence.filterIsInstance<T>()

val MarkdownElement.plainText: String get() {
    val sequence = this.asSequence
    return buildString {
        for (node in sequence) when (node) {
            is MarkdownText -> append(node.content)
            is MarkdownCode -> append(node.content)
            is MarkdownCodeBlock -> {
                if (isNotEmpty() && last() != '\n') append('\n')
                append(node.content)
            }
            is MarkdownParagraph, is MarkdownHeading, is MarkdownTableRow -> if (isNotEmpty() && last() != '\n') append('\n')
            is MarkdownSoftBreak, is MarkdownHardBreak -> append('\n')
            is MarkdownHtmlInline -> append(node.content)
            is MarkdownHtmlBlock -> {
                if (isNotEmpty() && last() != '\n') append('\n')
                append(node.content)
            }
            else -> Unit
        }
    }
}

val MarkdownElement?.Document: MarkdownDocument get() = this as MarkdownDocument
val MarkdownElement?.DocumentNull: MarkdownDocument? get() = this as? MarkdownDocument
val MarkdownElement?.Paragraph: MarkdownParagraph get() = this as MarkdownParagraph
val MarkdownElement?.ParagraphNull: MarkdownParagraph? get() = this as? MarkdownParagraph
val MarkdownElement?.Heading: MarkdownHeading get() = this as MarkdownHeading
val MarkdownElement?.HeadingNull: MarkdownHeading? get() = this as? MarkdownHeading
val MarkdownElement?.List: MarkdownList get() = this as MarkdownList
val MarkdownElement?.ListNull: MarkdownList? get() = this as? MarkdownList
val MarkdownElement?.Text: String get() = (this as MarkdownText).content
val MarkdownElement?.TextNull: String? get() = (this as? MarkdownText)?.content