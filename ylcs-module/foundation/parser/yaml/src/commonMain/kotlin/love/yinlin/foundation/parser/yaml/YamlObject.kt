package love.yinlin.foundation.parser.yaml

class YamlObject private constructor(
    private val content: Map<String, YamlElement>,
    @Suppress("unused") owned: Unit,
) : YamlElement(), Map<String, YamlElement> by content {
    constructor(content: Map<String, YamlElement>) : this(content.toMap(), Unit)

    fun obj(name: String): YamlObject = this[name] as YamlObject
    fun arr(name: String): YamlArray = this[name] as YamlArray

    override fun equals(other: Any?): Boolean = other is YamlObject && content == other.content
    override fun hashCode(): Int = content.hashCode()

    internal companion object {
        fun owned(content: Map<String, YamlElement>): YamlObject = YamlObject(content, Unit)
    }
}