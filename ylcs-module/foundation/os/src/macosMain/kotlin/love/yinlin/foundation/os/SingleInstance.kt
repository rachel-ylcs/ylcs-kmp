package love.yinlin.foundation.os

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.foundation.os.internal.RachelSingleInstanceCreatePort

@OptIn(ExperimentalForeignApi::class)
actual object SingleInstance {
    private var appMessagePort: COpaquePointer? = null

    actual fun run(key: String): Boolean {
        if (appMessagePort != null || key.isEmpty() || '\u0000' in key) return false
        val port = RachelSingleInstanceCreatePort(key) ?: return false
        appMessagePort = port
        return true
    }
}
