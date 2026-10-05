package love.yinlin.foundation.parser.yaml

class YamlArray private constructor(
    private val content: List<YamlElement>,
    @Suppress("unused") owned: Unit,
) : YamlElement(), List<YamlElement> by content {
    constructor(content: List<YamlElement>) : this(content.toList(), Unit)

    override fun equals(other: Any?): Boolean = other is YamlArray && content == other.content
    override fun hashCode(): Int = content.hashCode()

    internal companion object {
        fun owned(content: List<YamlElement>): YamlArray = YamlArray(content, Unit)
    }
}