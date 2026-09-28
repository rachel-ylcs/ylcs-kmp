package love.yinlin.foundation.cryptography

import java.security.SecureRandom

private val secureRandom by lazy { SecureRandom() }

internal actual fun secureRandomBytes(size: Int): ByteArray {
    val data = ByteArray(size)
    secureRandom.nextBytes(data)
    return data
}