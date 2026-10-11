package love.yinlin.foundation.cryptography

import love.yinlin.encoding.convert

abstract class ByteDigest : Digest {
    abstract fun encode(data: ByteArray): ByteArray

    fun encode(data: String): ByteArray = encode(data.convert())

    fun encodeToHex(data: ByteArray, format: HexFormat = HexFormat.Default): String = encode(data).toHexString(format)

    fun encodeToHex(data: String, format: HexFormat = HexFormat.Default): String = encode(data.convert()).toHexString(format)
}