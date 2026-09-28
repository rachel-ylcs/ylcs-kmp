package love.yinlin.foundation.cryptography

abstract class SymmetricCipher : Cipher() {
    final override fun decode(data: ByteArray): ByteArray = encode(data)
}