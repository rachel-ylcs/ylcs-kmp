package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.image.decoder.normalizedFrameDuration
import love.yinlin.extension.forever
import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

@Stable
internal class GifInfo(bytes: ByteArray, firstFrameOnly: Boolean = false) {
    companion object {
        private fun readGifPalette(input: ByteArrayIO, start: Int, count: Int): IntArray = IntArray(count) { index ->
            val offset = start + index * 3
            (0xff000000u.toInt()) or (input.readUByte(offset).toInt() shl 16) or (input.readUByte(offset + 1).toInt() shl 8) or input.readUByte(offset + 2).toInt()
        }

        private fun skipGifBlocks(input: ByteArrayIO, start: Int): Int {
            var offset = start

            forever {
                val length = input.readUByte(offset++).toInt()
                if (length == 0) return offset
                require(length <= input.size - offset) { "Truncated GIF sub-block" }
                offset += length
            }
        }
    }

    val size: IntSize
    val durations: IntArray
    val repeatCount: Int
    val frames: List<GifFrame>
    val background: Int

    init {
        val input = ByteArrayIO(bytes)
        require(input.readString(0, 6) in ["GIF87a", "GIF89a"]) { "Invalid GIF header" }
        size = IntSize(input.readUShort(6, Endian.LITTLE).toInt(), input.readUShort(8, Endian.LITTLE).toInt())
        require(size.width > 0 && size.height > 0)
        val flags = input.readUByte(10).toInt()
        val globalPalette = if (flags and 0x80 != 0) readGifPalette(input, 13, 2 shl (flags and 7)) else IntArray(0)
        val backgroundIndex = input.readUByte(11).toInt()
        var offset = 13 + if (flags and 0x80 != 0) 3 * (2 shl (flags and 7)) else 0
        var delay = 0
        var repeats = 0
        var disposal = 0
        var transparent = -1
        var ended = false
        val result: MutableList<GifFrame> = []

        while (offset < bytes.size && !ended) {
            when (input.readUByte(offset++).toInt()) {
                0x3b -> ended = true
                0x2c -> {
                    val x = input.readUShort(offset, Endian.LITTLE).toInt()
                    val y = input.readUShort(offset + 2, Endian.LITTLE).toInt()
                    val width = input.readUShort(offset + 4, Endian.LITTLE).toInt()
                    val height = input.readUShort(offset + 6, Endian.LITTLE).toInt()
                    require(width > 0 && height > 0 && x + width <= size.width && y + height <= size.height)

                    val local = input.readUByte(offset + 8).toInt()
                    offset += 9
                    val palette = if (local and 0x80 != 0) {
                        val count = 2 shl (local and 7)
                        readGifPalette(input, offset, count).also { offset += count * 3 }
                    } else globalPalette
                    require(palette.isNotEmpty())

                    val codeSize = input.readUByte(offset++).toInt()
                    require(codeSize in 2 .. 8)

                    val dataOffset = offset
                    offset = skipGifBlocks(input, dataOffset)
                    result += GifFrame(x, y, width, height, normalizedFrameDuration(delay), disposal, transparent, local and 0x40 != 0, palette, codeSize, dataOffset)
                    if (firstFrameOnly) {
                        ended = true
                        break
                    }
                    require(result.size <= 10000) { "GIF has too many frames" }

                    delay = 0
                    disposal = 0
                    transparent = -1
                }
                0x21 -> {
                    val label = input.readUByte(offset++).toInt()
                    when (label) {
                        0xf9 -> {
                            require(input.readUByte(offset).toInt() == 4)

                            val control = input.readUByte(offset + 1).toInt()
                            disposal = (control ushr 2) and 7
                            require(disposal <= 3)

                            transparent = if (control and 1 != 0) input.readUByte(offset + 4).toInt() else -1
                            delay = input.readUShort(offset + 2, Endian.LITTLE).toInt() * 10
                            require(input.readUByte(offset + 5).toInt() == 0)

                            offset += 6
                        }
                        0xff -> {
                            val length = input.readUByte(offset++).toInt()
                            val application = input.readString(offset, length)
                            offset += length

                            if (application == "NETSCAPE2.0" || application == "ANIMEXTS1.0") {
                                require(input.readUByte(offset).toInt() == 3 && input.readUByte(offset + 1).toInt() == 1)
                                val loops = input.readUShort(offset + 2, Endian.LITTLE).toInt()
                                repeats = if (loops == 0) -1 else loops
                            }
                            offset = skipGifBlocks(input, offset)
                        }
                        else -> offset = skipGifBlocks(input, offset)
                    }
                }
                else -> error("Invalid GIF block")
            }
        }

        require(ended && result.isNotEmpty()) { "Truncated GIF" }

        frames = result
        background = if (frames[0].transparent >= 0) 0 else globalPalette.getOrElse(backgroundIndex) { 0 }
        durations = result.map { it.duration }.toIntArray()
        repeatCount = repeats
    }
}