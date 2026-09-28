package love.yinlin.foundation.cryptography

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCRandomGenerateBytes
import platform.CoreCrypto.kCCSuccess

@OptIn(ExperimentalForeignApi::class)
internal actual fun secureRandomBytes(size: Int): ByteArray {
    val data = ByteArray(size)
    val status = data.usePinned { pinned ->
        CCRandomGenerateBytes(pinned.addressOf(0), size.convert())
    }
    check(status == kCCSuccess) { "CCRandomGenerateBytes failed: $status" }
    return data
}