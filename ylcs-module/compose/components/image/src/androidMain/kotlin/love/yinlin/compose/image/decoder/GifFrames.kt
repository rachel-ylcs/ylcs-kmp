package love.yinlin.compose.image.decoder

import kotlinx.coroutines.ensureActive
import love.yinlin.compose.image.data.GifFrame
import love.yinlin.compose.image.data.GifInfo
import kotlin.coroutines.CoroutineContext

/**
 * GIF89a LZW 与帧合成。仅保留当前画布、disposal=3 所需的前一画布和当前帧索引。
 * frame 的返回数组借用到下一次调用，调用方必须复制或立即上传到自己的位图。
 */
internal class GifFrames(private val bytes: ByteArray, private val info: GifInfo) {
    companion object {
        private fun decodeGifIndices(bytes: ByteArray, frame: GifFrame, context: CoroutineContext): ByteArray {
            context.ensureActive()
            val reader = GifCodeReader(bytes, frame.dataOffset)
            val clear = 1 shl frame.codeSize
            val end = clear + 1
            val prefix = IntArray(4096)
            val suffix = ByteArray(4096)
            val stack = ByteArray(4097)
            val output = ByteArray(frame.width * frame.height)

            for (index in 0 ..< clear) suffix[index] = index.toByte()

            var available = clear + 2
            var bits = frame.codeSize + 1
            var old = -1
            var first = 0
            var written = 0
            var codes = 0

            while (true) {
                if (codes++ and 0xFF == 0) context.ensureActive()
                var code = reader.code(bits)
                require(code >= 0) { "Truncated GIF LZW data" }

                if (code == clear) {
                    available = clear + 2
                    bits = frame.codeSize + 1
                    old = -1
                    continue
                }

                if (code == end) break
                require(code <= available && code < 4096) { "Invalid GIF LZW code" }

                if (old == -1) {
                    require(code < clear && written < output.size)
                    output[written++] = code.toByte()
                    first = code
                    old = code
                    continue
                }

                val incoming = code
                var top = 0
                if (code == available) {
                    stack[top++] = first.toByte()
                    code = old
                }

                while (code >= clear) {
                    require(code < available && top < 4096) { "Invalid GIF LZW chain" }
                    stack[top++] = suffix[code]
                    code = prefix[code]
                }

                first = code
                stack[top++] = code.toByte()
                require(top <= output.size - written) { "GIF LZW output exceeds its frame" }

                while (top > 0) output[written++] = stack[--top]

                if (available < 4096) {
                    prefix[available] = old
                    suffix[available++] = first.toByte()
                    if (available == (1 shl bits) && bits < 12) bits++
                }

                old = incoming
            }

            require(written == output.size) { "Incomplete GIF LZW output" }
            return output
        }
    }

    private val pixels = IntArray(info.size.width * info.size.height)
    private var previous = -1
    private var restore: IntArray? = null

    fun frame(index: Int, context: CoroutineContext): IntArray {
        require(index in info.frames.indices)
        context.ensureActive()

        if (index <= previous) previous = -1
        if (previous == -1) {
            pixels.fill(info.background)
            restore = null
        }

        for (position in previous + 1 .. index) {
            context.ensureActive()
            if (position > 0) {
                val before = info.frames[position - 1]
                when (before.disposal) {
                    2 -> {
                        val color = if (before.transparent >= 0) 0 else info.background
                        for (row in before.y ..< before.y + before.height) {
                            context.ensureActive()
                            val offset = row * info.size.width + before.x
                            pixels.fill(color, offset, offset + before.width)
                        }
                    }
                    3 -> restore?.copyInto(pixels)
                }
            }

            val frame = info.frames[position]
            restore = if (frame.disposal == 3) pixels.copyOf() else null
            val indices = decodeGifIndices(bytes, frame, context)
            var input = 0

            if (frame.interlaced) {
                val starts: IntArray = [0, 4, 2, 1]
                val steps: IntArray = [8, 8, 4, 2]
                for (pass in starts.indices) {
                    var row = starts[pass]
                    while (row < frame.height) {
                        drawRow(frame, row, indices, input, context)
                        input += frame.width
                        row += steps[pass]
                    }
                }
            }
            else {
                for (row in 0 ..< frame.height) {
                    drawRow(frame, row, indices, input, context)
                    input += frame.width
                }
            }
        }

        context.ensureActive()
        previous = index
        return pixels
    }

    private fun drawRow(frame: GifFrame, row: Int, indices: ByteArray, input: Int, context: CoroutineContext) {
        val output = (frame.y + row) * info.size.width + frame.x

        for (column in 0 ..< frame.width) {
            if (column and 0x3FF == 0) context.ensureActive()
            val color = indices[input + column].toInt() and 0xff
            if (color != frame.transparent) {
                require(color < frame.palette.size) { "GIF palette index out of bounds" }
                pixels[output + column] = frame.palette[color]
            }
        }
    }
}