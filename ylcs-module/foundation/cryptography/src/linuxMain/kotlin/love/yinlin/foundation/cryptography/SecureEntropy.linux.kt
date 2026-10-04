package love.yinlin.foundation.cryptography

import kotlinx.cinterop.*
import platform.linux.SYS_getrandom
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual object SecureEntropy {
    actual fun bytes(size: Int): ByteArray {
        val data = ByteArray(size)

        data.usePinned { pinned ->
            var offset = 0

            while (offset < size) {
                val count = syscall(
                    SYS_getrandom.convert(),
                    pinned.addressOf(offset),
                    (size - offset).convert<size_t>(),
                    0U
                ).toInt()

                if (count < 0) {
                    if (errno == EINTR) continue
                    error("getrandom failed: errno=$errno")
                }

                check(count > 0) { "getrandom returned 0" }

                offset += count
            }
        }

        return data
    }
}