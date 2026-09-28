package love.yinlin.foundation.cryptography

abstract class StringDigest : Digest {
    abstract fun encode(data: String): String

    fun encode(data: ByteArray): String = encode(data.decodeToString())
}