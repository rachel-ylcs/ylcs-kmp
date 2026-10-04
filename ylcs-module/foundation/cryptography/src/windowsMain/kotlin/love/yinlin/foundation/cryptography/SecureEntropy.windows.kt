package love.yinlin.foundation.cryptography

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.windows.BCRYPT_USE_SYSTEM_PREFERRED_RNG
import platform.windows.BCryptGenRandom

@OptIn(ExperimentalForeignApi::class)
actual object SecureEntropy {
    actual fun bytes(size: Int): ByteArray {
        val data = ByteArray(size)
        val status = data.usePinned { pinned ->
            BCryptGenRandom(
                null,
                pinned.addressOf(0).reinterpret(),
                size.toUInt(),
                BCRYPT_USE_SYSTEM_PREFERRED_RNG.toUInt(),
            )
        }
        check(status == 0) { "BCryptGenRandom failed: $status" }
        return data
    }
}