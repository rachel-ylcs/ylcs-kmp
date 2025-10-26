package love.yinlin.uri

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(Scheme.Serializer::class)
data class Scheme(val name: String) {
    companion object {
        val Http = Scheme("http")
        val Https = Scheme("https")
        val File = Scheme("file")
        val Content = Scheme("content")
        val Package = Scheme("scheme")
        val Rachel = Scheme("rachel")
        val NetEaseCloud = Scheme("nec")
        val QQMusic = Scheme("qm")
        val Taobao = Scheme("taobao")
        val QQ = Scheme("mqqapi")
    }

    override fun toString(): String = name

    object Serializer : KSerializer<Scheme> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("json.convert.Scheme", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: Scheme) = encoder.encodeString(value.name)
        override fun deserialize(decoder: Decoder): Scheme = Scheme(decoder.decodeString())
    }
}