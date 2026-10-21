package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.image.decoder.normalizedFrameDuration
import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

@Stable
internal class WebpInfo(bytes: ByteArray, firstFrameOnly: Boolean = false) {
    companion object {
        private fun ByteArrayIO.readUInt24(offset: Int): Int = readUByte(offset).toInt() or (readUByte(offset + 1).toInt() shl 8) or (readUByte(offset + 2).toInt() shl 16)

        private fun validateWebpFrame(input: ByteArrayIO, start: Int, end: Int) {
            var offset = start
            var pixels = false

            while (offset < end) {
                require(end - offset >= 8)

                val tag = input.readString(offset, 4)
                val length = input.readUInt(offset + 4, Endian.LITTLE).toLong()
                require(length > 0 && length <= end.toLong() - offset - 8)

                when (tag) {
                    "ALPH" -> require(!pixels)
                    "VP8 ", "VP8L" -> {
                        require(!pixels)
                        pixels = true
                    }
                    else -> error("Invalid WebP frame chunk")
                }

                val next = offset.toLong() + 8 + length + (length and 1)
                require(next <= end)
                offset = next.toInt()
            }
            require(pixels)
        }
    }

    val size: IntSize
    val background: Int
    val frames: List<WebpFrame>
    val repeatCount: Int

    init {
        val input = ByteArrayIO(bytes)
        require(input.readString(0, 4) == "RIFF" && input.readString(8, 4) == "WEBP")
        require(input.readUInt(4, Endian.LITTLE).toLong() + 8 == bytes.size.toLong()) { "Truncated WebP RIFF" }

        var offset = 12
        var canvas = IntSize.Zero
        var color = 0
        var repeats = 0
        var animated = false
        var animationHeader = false
        val result: MutableList<WebpFrame> = []

        while (offset < bytes.size) {
            val tag = input.readString(offset, 4)
            val length = input.readUInt(offset + 4, Endian.LITTLE).toLong()
            require(length <= bytes.size.toLong() - offset - 8) { "Truncated WebP chunk" }

            val begin = offset + 8
            when (tag) {
                "VP8X" -> {
                    require(length == 10L && canvas == IntSize.Zero)
                    animated = input.readUByte(begin).toInt() and 2 != 0
                    canvas = IntSize(input.readUInt24(begin + 4) + 1, input.readUInt24(begin + 7) + 1)
                }
                "ANIM" -> {
                    require(length == 6L && animated && !animationHeader)
                    color = input.readInt(begin, Endian.LITTLE)

                    val loops = input.readUShort(begin + 4, Endian.LITTLE).toInt()
                    repeats = if (loops == 0) -1 else loops - 1
                    animationHeader = true
                }
                "ANMF" -> {
                    require(length > 16L && animated && animationHeader)

                    val x = input.readUInt24(begin) * 2
                    val y = input.readUInt24(begin + 3) * 2
                    val width = input.readUInt24(begin + 6) + 1
                    val height = input.readUInt24(begin + 9) + 1
                    val duration = normalizedFrameDuration(input.readUInt24(begin + 12))
                    val flags = input.readUByte(begin + 15).toInt()
                    require(flags and 0xfc == 0 && width <= canvas.width - x && height <= canvas.height - y)

                    validateWebpFrame(input, begin + 16, begin + length.toInt())
                    result += WebpFrame(x, y, width, height, duration, flags and 1 != 0, flags and 2 == 0, begin + 16, length.toInt() - 16)

                    if (firstFrameOnly) break

                    require(result.size <= 10000) { "WebP has too many frames" }
                }
            }

            val next = offset.toLong() + 8 + length + (length and 1)
            require(next <= bytes.size) { "Missing WebP chunk padding" }
            offset = next.toInt()
        }

        require(animated && animationHeader && result.isNotEmpty()) { "Not an animated WebP" }

        size = canvas
        background = color
        repeatCount = repeats
        frames = result
    }
}