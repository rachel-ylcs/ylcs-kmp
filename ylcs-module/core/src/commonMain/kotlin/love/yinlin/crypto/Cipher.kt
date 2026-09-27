package love.yinlin.crypto

abstract class Cipher {
    abstract fun encode(data: ByteArray): ByteArray
    abstract fun decode(data: ByteArray): ByteArray

    fun encode(data: String): ByteArray = encode(data.encodeToByteArray())

    fun encodeToHex(data: ByteArray, format: HexFormat = HexFormat.Default): String = encode(data).toHexString(format)

    fun encodeToHex(data: String, format: HexFormat = HexFormat.Default): String = encode(data.encodeToByteArray()).toHexString(format)

    fun decodeToString(data: ByteArray): String = decode(data).decodeToString()

    fun decodeFromHex(data: String, format: HexFormat = HexFormat.Default): ByteArray = decode(data.hexToByteArray(format))

    fun decodeFromHexToString(data: String, format: HexFormat = HexFormat.Default): String = decode(data.hexToByteArray(format)).decodeToString()
}