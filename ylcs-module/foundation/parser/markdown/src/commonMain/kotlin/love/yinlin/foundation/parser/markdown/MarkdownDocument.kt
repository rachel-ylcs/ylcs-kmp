package love.yinlin.foundation.parser.markdown

import kotlinx.serialization.Serializable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.extension.StringSerializer
import love.yinlin.reflect.metaClassName

@Serializable(MarkdownDocument.Serializer::class)
class MarkdownDocument private constructor(
    override val children: List<MarkdownBlock>,
    @Suppress("unused") owned: Unit,
) : MarkdownElement(), List<MarkdownBlock> by children {
    @OptIn(CompatibleRachelApi::class)
    object Serializer : StringSerializer<MarkdownDocument>(metaClassName<MarkdownDocument>()) {
        override fun encode(value: MarkdownDocument): String = Markdown.encodeToString(value)
        override fun decode(value: String): MarkdownDocument = Markdown.parse(value)
    }

    constructor(content: List<MarkdownBlock>) : this(content.toList(), Unit)

    override fun equals(other: Any?): Boolean = other is MarkdownDocument && children == other.children
    override fun hashCode(): Int = children.hashCode()
}