package love.yinlin.collection

import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = StableMap.Serializer::class)
@Stable
class StableMap<K, V>(private val instance: Map<K, V>) : Map<K, V> by instance {
    class Serializer<K, V>(keySerializer: KSerializer<K>, valueSerializer: KSerializer<V>) : KSerializer<StableMap<K, V>> {
        private val delegate = MapSerializer(keySerializer, valueSerializer)
        override val descriptor: SerialDescriptor = delegate.descriptor
        override fun serialize(encoder: Encoder, value: StableMap<K, V>) = delegate.serialize(encoder, value.instance)
        override fun deserialize(decoder: Decoder): StableMap<K, V> = StableMap(delegate.deserialize(decoder))
    }

    override fun equals(other: Any?): Boolean = instance == other
    override fun hashCode(): Int = instance.hashCode()
}

inline fun <reified K, reified V> Map<K, V>.toStableMap(): StableMap<K, V> = StableMap(this)
inline fun <reified K, reified V> emptyStableMap(): StableMap<K, V> = StableMap(emptyMap())
inline fun <reified K, reified V> stableMapOf(): StableMap<K, V> = StableMap(emptyMap())
inline fun <reified K, reified V> stableMapOf(element: Pair<K, V>): StableMap<K, V> = StableMap(mapOf(element))
inline fun <reified K, reified V> stableMapOf(vararg element: Pair<K, V>): StableMap<K, V> = StableMap(mapOf(*element))