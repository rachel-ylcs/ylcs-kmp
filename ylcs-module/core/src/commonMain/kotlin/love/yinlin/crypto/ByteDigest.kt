package love.yinlin.crypto

abstract class ByteDigest : Digest {
    abstract fun encode(data: ByteArray): ByteArray

    fun encode(data: String): ByteArray = encode(data.encodeToByteArray())

    fun encodeToHex(data: ByteArray, format: HexFormat = HexFormat.Default): String = encode(data).toHexString(format)

    fun encodeToHex(data: String, format: HexFormat = HexFormat.Default): String = encode(data.encodeToByteArray()).toHexString(format)
}