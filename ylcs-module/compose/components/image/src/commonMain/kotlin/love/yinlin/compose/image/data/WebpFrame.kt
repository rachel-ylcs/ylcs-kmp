package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

@Stable
internal class WebpFrame(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val duration: Int,
    val dispose: Boolean,
    val blend: Boolean,
    private val offset: Int,
    private val length: Int,
) {
    companion object {
        private val RIFF: ByteArray = [0x52, 0x49, 0x46, 0x46]
        private val WEBP_VP8X: ByteArray = [0x57, 0x45, 0x42, 0x50, 0x56, 0x50, 0x38, 0x58]

        private fun ByteArrayIO.writeUInt24(offset: Int, value: Int) {
            writeByte(offset, value.toByte())
            writeByte(offset + 1, (value ushr 8).toByte())
            writeByte(offset + 2, (value ushr 16).toByte())
        }
    }

    fun encoded(bytes: ByteArray): ByteArray = ByteArrayIO(30 + length).write {
        writeByteArray(0, RIFF)
        writeInt(4, 22 + length, Endian.LITTLE)
        writeByteArray(8, WEBP_VP8X)
        writeInt(16, 10, Endian.LITTLE)
        writeByte(20, 0x10)
        writeUInt24(24, width - 1)
        writeUInt24(27, height - 1)
        writeByteArray(30, bytes, offset, offset + length)
    }
}