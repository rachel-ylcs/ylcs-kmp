package love.yinlin.crypto

fun interface Digest {
    fun encode(data: ByteArray): ByteArray
}