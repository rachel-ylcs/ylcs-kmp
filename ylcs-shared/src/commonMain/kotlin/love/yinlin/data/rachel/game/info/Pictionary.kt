package love.yinlin.data.rachel.game.info

import androidx.compose.runtime.Stable
import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.readByteArray
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.yinlin.data.rachel.game.RankConfig
import kotlin.io.encoding.Base64

@Stable
@Serializable(PaintPath.Serializer::class)
@SerialName("PP")
data class PaintPath(
    @SerialName("p") val paths: List<Long>,
    @SerialName("w") val width: Float,
    @SerialName("c") val color: Int
) {
    object Serializer : KSerializer<PaintPath> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PP") {
            element<String>("p")
            element<Float>("w")
            element<Int>("c")
        }

        override fun serialize(encoder: Encoder, value: PaintPath) {
            encoder.beginStructure(descriptor).run {
                val buffer = Buffer()
                for (path in value.paths) buffer.writeLong(path)
                encodeStringElement(descriptor, 0, Base64.encode(buffer.readByteArray()))
                encodeFloatElement(descriptor, 1, value.width)
                encodeIntElement(descriptor, 2, value.color)
                endStructure(descriptor)
            }
        }

        @OptIn(UnsafeIoApi::class)
        override fun deserialize(decoder: Decoder): PaintPath = decoder.beginStructure(descriptor).run {
            val paths = mutableListOf<Long>()
            var width = 1f
            var color = 0
            val buffer = Buffer()
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> {
                        val base64Str = decodeStringElement(descriptor, 0)
                        val bytes = Base64.decode(base64Str)
                        UnsafeBufferOperations.moveToTail(buffer, bytes)
                        repeat(bytes.size / 8) { paths += buffer.readLong() }
                    }
                    1 -> width = decodeFloatElement(descriptor, 1)
                    2 -> color = decodeIntElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("unexpected index: $index")
                }
            }
            endStructure(descriptor)
            PaintPath(paths, width, color)
        }
    }
}

@Stable
@Suppress("MayBeConstant")
data object PConfig : RankConfig() {
    val minAnswerLength: Int = 1 // 最小答案长度
    val maxAnswerLength: Int = 12 // 最大答案长度
}