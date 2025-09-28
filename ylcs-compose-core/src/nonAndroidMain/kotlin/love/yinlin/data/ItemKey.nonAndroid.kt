package love.yinlin.data

import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Stable
@Serializable(with = ItemKey.Serializer::class)
actual data class ItemKey actual constructor(val value: String) {
    object Serializer : KSerializer<ItemKey> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("json.convert.ItemKey", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: ItemKey) = encoder.encodeString(value.value)
        override fun deserialize(decoder: Decoder): ItemKey = ItemKey(decoder.decodeString())
    }
}