package love.yinlin.foundation.parser.toml

class TomlObject private constructor(
    private val content: Map<String, TomlElement>,
    @Suppress("unused") owned: Unit,
) : TomlElement(), Map<String, TomlElement> by content {
    constructor(content: Map<String, TomlElement>) : this(content.toMap(), Unit)

    fun obj(name: String): TomlObject = this[name] as TomlObject
    fun arr(name: String): TomlArray = this[name] as TomlArray

    override fun equals(other: Any?): Boolean = other is TomlObject && content == other.content
    override fun hashCode(): Int = content.hashCode()

    internal companion object {
        fun owned(content: Map<String, TomlElement>): TomlObject = TomlObject(content, Unit)
    }
}