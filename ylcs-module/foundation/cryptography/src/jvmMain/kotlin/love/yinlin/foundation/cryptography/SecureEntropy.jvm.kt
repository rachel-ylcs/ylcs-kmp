package love.yinlin.foundation.cryptography

import java.security.SecureRandom

actual object SecureEntropy {
    private val secureRandom by lazy { SecureRandom() }

    actual fun bytes(size: Int): ByteArray {
        val data = ByteArray(size)
        secureRandom.nextBytes(data)
        return data
    }
}