package love.yinlin.ui.component.container

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
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
import love.yinlin.common.Colors
import love.yinlin.extension.rememberState
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
                value.paths.fastForEach { buffer.writeLong(it) }
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
            var color = Colors.Black.toArgb()
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

private fun DrawScope.drawPaintPath(paths: List<Long>, width: Float, color: Color) {
    val path = Path().apply {
        paths.firstOrNull()?.let { first ->
            var x = unpackFloat1(first)
            var y = unpackFloat2(first)
            moveTo(x, y)
            for (i in 1 ..< paths.size - 1) {
                x = unpackFloat1(paths[i])
                y = unpackFloat2(paths[i])
                lineTo(x, y)
            }
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

@Composable
fun PaintCanvas(
    items: List<PaintPath>,
    color: Color,
    width: Float,
    onPathAdded: (PaintPath) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val currentPath = remember { mutableStateListOf<Long>() }
        var currentOffset: Offset? by rememberState { null }

        Canvas(modifier = Modifier.matchParentSize()
            .background(Colors.White)
            .pointerInput(enabled, color, width, onPathAdded) {
                if (enabled) detectDragGestures(
                    onDragStart = { currentPath += packFloats(it.x, it.y) },
                    onDragEnd = {
                        val distinctPath = when (currentPath.size) {
                            in 0 .. 1 -> null
                            2 -> listOf(currentPath[0], currentPath[1])
                            else -> {
                                val last = currentPath.last()
                                var index = currentPath.size - 2
                                while (index >= 0 && currentPath[index] == last) index--
                                currentPath.take(index + 2)
                            }
                        }
                        distinctPath?.let { onPathAdded(PaintPath(it, width, color.toArgb())) }
                        currentPath.clear()
                    },
                    onDrag = { v, _ -> currentOffset = v.position }
                )
            }.pointerInput(enabled, color, width, onPathAdded) {
                if (enabled) detectTapGestures {
                    onPathAdded(PaintPath(listOf(
                        packFloats(it.x, it.y),
                        packFloats(it.x, it.y)
                    ), width, color.toArgb()))
                }
            }
        ) {
            items.fastForEach { (paths, width, color) ->
                drawPaintPath(paths, width, Color(color))
            }
            currentOffset?.let { offset ->
                if (currentPath.isNotEmpty()) {
                    currentPath += packFloats(offset.x, offset.y)
                    drawPaintPath(currentPath, width, color)
                }
            }
        }
    }
}