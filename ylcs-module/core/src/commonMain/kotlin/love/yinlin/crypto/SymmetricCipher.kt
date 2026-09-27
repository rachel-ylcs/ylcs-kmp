package love.yinlin.crypto

abstract class SymmetricCipher : Cipher() {
    final override fun decode(data: ByteArray): ByteArray = encode(data)
}