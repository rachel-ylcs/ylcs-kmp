package love.yinlin.compose.collection

import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = StableList.Serializer::class)
@Stable
class StableList<T>(private val instance: List<T>) : List<T> by instance {
    class Serializer<T>(elementSerializer: KSerializer<T>) : KSerializer<StableList<T>> {
        private val delegate = ListSerializer(elementSerializer)
        override val descriptor: SerialDescriptor = delegate.descriptor
        override fun serialize(encoder: Encoder, value: StableList<T>) = delegate.serialize(encoder, value.instance)
        override fun deserialize(decoder: Decoder): StableList<T> = StableList(delegate.deserialize(decoder))
    }

    override fun equals(other: Any?): Boolean = instance == other
    override fun hashCode(): Int = instance.hashCode()
}

inline fun <reified T> List<T>.toStableList(): StableList<T> = StableList(this)
inline fun <reified T> emptyStableList(): StableList<T> = StableList(emptyList())
inline fun <reified T> stableListOf(): StableList<T> = StableList(emptyList())
inline fun <reified T> stableListOf(element: T): StableList<T> = StableList(listOf(element))
inline fun <reified T> stableListOf(vararg element: T): StableList<T> = StableList(listOf(*element))