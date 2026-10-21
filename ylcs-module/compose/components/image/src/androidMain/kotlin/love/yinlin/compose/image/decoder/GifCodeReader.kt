package love.yinlin.compose.image.decoder

internal class GifCodeReader(private val bytes: ByteArray, private var offset: Int) {
    private var remaining = 0
    private var buffer = 0
    private var bitCount = 0
    private var ended = false

    fun code(bits: Int): Int {
        while (bitCount < bits) {
            if (ended || offset >= bytes.size) return -1
            if (remaining == 0) {
                remaining = bytes[offset++].toInt() and 0xff
                if (remaining == 0) {
                    ended = true
                    return -1
                }
            }
            require(offset < bytes.size)
            buffer = buffer or ((bytes[offset++].toInt() and 0xff) shl bitCount)
            remaining--
            bitCount += 8
        }
        val value = buffer and ((1 shl bits) - 1)
        buffer = buffer ushr bits
        bitCount -= bits
        return value
    }
}