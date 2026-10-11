package love.yinlin.foundation.parser.yaml

import kotlinx.serialization.Serializable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.extension.StringSerializer
import love.yinlin.reflect.metaClassName

@Serializable(YamlDocument.Serializer::class)
class YamlDocument(val root: YamlElement) : YamlElement() {
    init {
        require(root !is YamlDocument) { "A YAML document cannot have a document root" }
    }

    @OptIn(CompatibleRachelApi::class)
    object Serializer : StringSerializer<YamlDocument>(metaClassName<YamlDocument>()) {
        override fun encode(value: YamlDocument): String = Yaml.encodeToString(value)
        override fun decode(value: String): YamlDocument = Yaml.parse(value)
    }

    override fun equals(other: Any?): Boolean = other is YamlDocument && root == other.root
    override fun hashCode(): Int = root.hashCode()
}
