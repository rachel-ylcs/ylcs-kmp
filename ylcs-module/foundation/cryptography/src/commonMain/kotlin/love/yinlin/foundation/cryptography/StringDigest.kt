package love.yinlin.foundation.cryptography

import love.yinlin.encoding.convert

abstract class StringDigest : Digest {
    abstract fun encode(data: String): String

    fun encode(data: ByteArray): String = encode(data.convert())
}