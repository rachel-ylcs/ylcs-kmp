package love.yinlin.crypto

interface Digest {
    fun encode(data: ByteArray): ByteArray
    fun encodeToString(data: ByteArray): String
    fun encodeToString(data: String): String
}