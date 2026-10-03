package love.yinlin.uri

import kotlinx.serialization.Serializable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.extension.StringSerializer
import love.yinlin.reflect.metaClassName

@Serializable(Scheme.Serializer::class)
data class Scheme(val name: String) {
    companion object {
        val Http = Scheme("http")
        val Https = Scheme("https")
        val File = Scheme("file")
        val Content = Scheme("content")
        val Package = Scheme("package")
        val Rachel = Scheme("rachel")
        val NetEaseCloud = Scheme("nec")
        val QQMusic = Scheme("qm")
        val Taobao = Scheme("taobao")
        val QQ = Scheme("mqqapi")
    }

    override fun toString(): String = name

    @OptIn(CompatibleRachelApi::class)
    object Serializer : StringSerializer<Scheme>(metaClassName<Scheme>()) {
        override fun encode(value: Scheme): String = value.name
        override fun decode(value: String): Scheme = Scheme(value)
    }
}