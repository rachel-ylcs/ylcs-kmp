package love.yinlin.compose.collection

import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = StableSet.Serializer::class)
@Stable
class StableSet<T>(private val instance: Set<T>) : Set<T> by instance {
    class Serializer<T>(elementSerializer: KSerializer<T>) : KSerializer<StableSet<T>> {
        private val delegate = SetSerializer(elementSerializer)
        override val descriptor: SerialDescriptor = delegate.descriptor
        override fun serialize(encoder: Encoder, value: StableSet<T>) = delegate.serialize(encoder, value.instance)
        override fun deserialize(decoder: Decoder): StableSet<T> = StableSet(delegate.deserialize(decoder))
    }

    override fun equals(other: Any?): Boolean = instance == other
    override fun hashCode(): Int = instance.hashCode()
}

inline fun <reified T> Set<T>.toStableSet(): StableSet<T> = StableSet(this)
inline fun <reified T> emptyStableSet(): StableSet<T> = StableSet(emptySet())
inline fun <reified T> stableSetOf(): StableSet<T> = StableSet(emptySet())
inline fun <reified T> stableSetOf(element: T): StableSet<T> = StableSet(setOf(element))
inline fun <reified T> stableSetOf(vararg element: T): StableSet<T> = StableSet(setOf(*element))