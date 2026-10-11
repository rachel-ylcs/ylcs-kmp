package love.yinlin.foundation.parser.arguments

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName
import love.yinlin.reflect.metaSimpleClassName

@Serializable(Argument.Serializer::class)
sealed interface Argument {
    object Serializer : KSerializer<Argument> {
        @OptIn(CompatibleRachelApi::class)
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(metaClassName<Argument>())
        override fun serialize(encoder: Encoder, value: Argument) = serializeClass(encoder, value)
        override fun deserialize(decoder: Decoder): Argument = deserializeClass(decoder)
    }

    val size: Int
    val first: String
    val firstOrNull: String?
    operator fun contains(value: String): Boolean
    operator fun get(index: Int): String
    fun toList(): List<String>

    @Serializable(Empty.Serializer::class)
    data object Empty : Argument {
        object Serializer : KSerializer<Empty> {
            @OptIn(CompatibleRachelApi::class)
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor(metaClassName<Empty>())
            override fun serialize(encoder: Encoder, value: Empty) = serializeClass(encoder, value)
            override fun deserialize(decoder: Decoder): Empty = deserializeClass(decoder) as Empty
        }

        override val size: Int = 0
        override val first: Nothing get() = throw ArgumentStateException("Argument has no value.")
        override val firstOrNull: String? = null
        override fun contains(value: String): Boolean = false
        override fun get(index: Int): Nothing = throw ArgumentStateException("Argument has no value.")
        override fun toList(): List<String> = []
        override fun toString(): String = "Argument.Empty"
    }

    @Serializable(Single.Serializer::class)
    class Single internal constructor(val value: String) : Argument {
        object Serializer : KSerializer<Single> {
            @OptIn(CompatibleRachelApi::class)
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor(metaClassName<Single>())
            override fun serialize(encoder: Encoder, value: Single) = serializeClass(encoder, value)
            override fun deserialize(decoder: Decoder): Single = deserializeClass(decoder) as Single
        }

        override val size: Int = 1
        override val first: String = value
        override val firstOrNull: String = value
        override fun contains(value: String): Boolean = this.value == value
        override fun get(index: Int): String = if (index == 0) value else throw ArgumentStateException("Argument value index $index is outside 0 until $size.")
        override fun toList(): List<String> = [value]
        override fun equals(other: Any?): Boolean = (other as? Single)?.value == value
        override fun hashCode(): Int = value.hashCode()
        override fun toString(): String = value
    }

    @Serializable(Values.Serializer::class)
    class Values internal constructor(val values: List<String>) : Argument {
        object Serializer : KSerializer<Values> {
            @OptIn(CompatibleRachelApi::class)
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor(metaClassName<Values>())
            override fun serialize(encoder: Encoder, value: Values) = serializeClass(encoder, value)
            override fun deserialize(decoder: Decoder): Values = deserializeClass(decoder) as Values
        }

        override val size: Int = values.size
        override val first: String = values[0]
        override val firstOrNull: String = values[0]
        override fun contains(value: String): Boolean = value in values
        override fun get(index: Int): String = values.getOrNull(index) ?: throw ArgumentStateException("Argument value index $index is outside 0 until $size.")
        override fun toList(): List<String> = values
        override fun equals(other: Any?): Boolean = (other as? Values)?.values == values
        override fun hashCode(): Int = values.hashCode()
        override fun toString(): String = values.toString()
    }

    companion object {
        private val StringListSerializer = ListSerializer(String.serializer())

        internal fun serializeClass(encoder: Encoder, value: Argument) {
            when (value) {
                is Empty -> {
                    encoder.encodeString(metaSimpleClassName<Empty>())
                }
                is Single -> {
                    encoder.encodeString(metaSimpleClassName<Single>())
                    encoder.encodeString(value.value)
                }
                is Values -> {
                    encoder.encodeString(metaSimpleClassName<Values>())
                    encoder.encodeSerializableValue(StringListSerializer, value.values)
                }
            }
        }

        internal fun deserializeClass(decoder: Decoder): Argument = when (val type = decoder.decodeString()) {
            metaSimpleClassName<Empty>() -> Empty
            metaSimpleClassName<Single>() -> Single(decoder.decodeString())
            metaSimpleClassName<Values>() -> Values(decoder.decodeSerializableValue(StringListSerializer))
            else -> throw SerializationException("Unsupported argument type $type")
        }

        private fun build(vararg values: String) = when (values.size) {
            0 -> Empty
            1 -> Single(values[0])
            else -> Values(values.toList())
        }
        private fun build(values: List<String>) = when (values.size) {
            0 -> Empty
            1 -> Single(values[0])
            else -> Values(values.toList())
        }
        operator fun invoke(): Argument = Empty
        operator fun invoke(value: String): Argument = Single(value)
        operator fun invoke(values: List<String>): Argument = build(values)
        operator fun invoke(vararg values: String): Argument = build(*values)
        operator fun of(): Argument = Empty
        operator fun of(value: String): Argument = Single(value)
        operator fun of(vararg values: String): Argument = build(*values)
    }
}
