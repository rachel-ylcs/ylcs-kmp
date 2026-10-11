package love.yinlin.extension

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

abstract class DelegateSerializer<T, R> : KSerializer<T> {
    abstract val delegate: KSerializer<R>
    abstract fun encode(value: T): R
    abstract fun decode(value: R): T
    final override val descriptor: SerialDescriptor get() = delegate.descriptor
    final override fun serialize(encoder: Encoder, value: T) = encoder.encodeSerializableValue(delegate, encode(value))
    final override fun deserialize(decoder: Decoder): T = decode(decoder.decodeSerializableValue(delegate))
}

abstract class ListDelegateSerializer<T, E>(elementSerializer: KSerializer<E>) : KSerializer<T> {
    abstract fun encode(value: T): List<E>
    abstract fun decode(value: List<E>): T

    private val delegate = ListSerializer(elementSerializer)
    final override val descriptor: SerialDescriptor = delegate.descriptor
    final override fun serialize(encoder: Encoder, value: T) = encoder.encodeSerializableValue(delegate, encode(value))
    final override fun deserialize(decoder: Decoder): T = decode(decoder.decodeSerializableValue(delegate))
}

abstract class MapDelegateSerializer<T, K, V>(keySerializer: KSerializer<K>, valueSerializer: KSerializer<V>) : KSerializer<T> {
    abstract fun encode(value: T): Map<K, V>
    abstract fun decode(value: Map<K, V>): T

    private val delegate = MapSerializer(keySerializer, valueSerializer)
    final override val descriptor: SerialDescriptor = delegate.descriptor
    final override fun serialize(encoder: Encoder, value: T) = encoder.encodeSerializableValue(delegate, encode(value))
    final override fun deserialize(decoder: Decoder): T = decode(decoder.decodeSerializableValue(delegate))
}

abstract class IntSerializer<T>(name: String) : KSerializer<T> {
    abstract fun encode(value: T): Int
    abstract fun decode(value: Int): T

    final override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("json.convert.$name", PrimitiveKind.INT)
    final override fun serialize(encoder: Encoder, value: T) = encoder.encodeInt(encode(value))
    final override fun deserialize(decoder: Decoder): T = decode(decoder.decodeInt())
}

abstract class StringSerializer<T>(name: String) : KSerializer<T> {
    abstract fun encode(value: T): String
    abstract fun decode(value: String): T

    final override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("json.convert.$name", PrimitiveKind.STRING)
    final override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(encode(value))
    final override fun deserialize(decoder: Decoder): T = decode(decoder.decodeString())
}

@OptIn(CompatibleRachelApi::class)
object ByteArraySerializer : StringSerializer<ByteArray>(metaClassName<ByteArray>()) {
    override fun encode(value: ByteArray): String = value.toHexString(format = HexFormat.UpperCase)
    override fun decode(value: String): ByteArray = value.hexToByteArray(format = HexFormat.UpperCase)
}