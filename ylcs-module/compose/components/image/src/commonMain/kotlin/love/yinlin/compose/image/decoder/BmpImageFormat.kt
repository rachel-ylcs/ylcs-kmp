package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian
import kotlin.math.abs

@Stable
object BmpImageFormat : ImageFormat {
    override fun matches(bytes: ByteArray): Boolean = bytes.size >= 2 && bytes[0] == 66.toByte() && bytes[1] == 77.toByte()

    internal fun bmpImageSize(bytes: ByteArray): IntSize {
        require(matches(bytes) && bytes.size >= 26) { "Invalid BMP header" }
        val input = ByteArrayIO(bytes)
        val headerSize = input.readUInt(14, Endian.LITTLE).toLong()
        require((headerSize == 12L || headerSize >= 16L) && headerSize <= bytes.size.toLong() - 14) { "Invalid BMP DIB header" }
        val offset = input.readUInt(10, Endian.LITTLE).toLong()
        require(offset >= 14L + headerSize && offset < bytes.size) { "Invalid BMP pixel offset" }
        val fileSize = input.readUInt(2, Endian.LITTLE).toLong()
        require(fileSize == 0L || fileSize in offset + 1 .. bytes.size.toLong()) { "Truncated BMP" }
        val core = headerSize == 12L
        val width = if (core) input.readUShort(18, Endian.LITTLE).toInt() else input.readInt(18, Endian.LITTLE)
        val height = if (core) input.readUShort(20, Endian.LITTLE).toInt() else input.readInt(22, Endian.LITTLE)
        val planes = input.readUShort(if (core) 22 else 26, Endian.LITTLE).toInt()
        require(width > 0 && height != 0 && height != Int.MIN_VALUE && planes == 1) { "Invalid BMP dimensions or planes" }
        return IntSize(width, abs(height))
    }

    override fun open(bytes: ByteArray, options: ImageDecodeOptions): ImageDecoder {
        // 平台支持 CORE / INFO、调色板和 bitfields；先校验尺寸，避免坏头部进入原生分配
        options.sampledSize(bmpImageSize(bytes))
        return openPlatformImageDecoder(bytes, options.staticOptions())
    }
}
