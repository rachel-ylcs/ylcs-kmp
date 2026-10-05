package love.yinlin.foundation.parser.toml

class TomlArray private constructor(
    private val content: List<TomlElement>,
    @Suppress("unused") owned: Unit,
) : TomlElement(), List<TomlElement> by content {
    constructor(content: List<TomlElement>) : this(content.toList(), Unit)

    override fun equals(other: Any?): Boolean = other is TomlArray && content == other.content
    override fun hashCode(): Int = content.hashCode()

    internal companion object {
        fun owned(content: List<TomlElement>): TomlArray = TomlArray(content, Unit)
    }
}