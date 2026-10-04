package love.yinlin.foundation.cryptography

expect object SecureEntropy {
    fun bytes(size: Int): ByteArray
}