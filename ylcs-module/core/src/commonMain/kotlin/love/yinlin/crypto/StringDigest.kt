package love.yinlin.crypto

abstract class StringDigest : Digest {
    abstract fun encode(data: String): String

    fun encode(data: ByteArray): String = encode(data.decodeToString())
}