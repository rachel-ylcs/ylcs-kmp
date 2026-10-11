package love.yinlin.foundation.parser.toml

import kotlinx.serialization.Serializable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.extension.StringSerializer
import love.yinlin.reflect.metaClassName

@Serializable(TomlDocument.Serializer::class)
class TomlDocument(val root: TomlObject) : TomlElement() {
    @OptIn(CompatibleRachelApi::class)
    object Serializer : StringSerializer<TomlDocument>(metaClassName<TomlDocument>()) {
        override fun encode(value: TomlDocument): String = Toml.encodeToString(value)
        override fun decode(value: String): TomlDocument = Toml.parse(value)
    }

    override fun equals(other: Any?): Boolean = other is TomlDocument && root == other.root
    override fun hashCode(): Int = root.hashCode()
}
