package love.yinlin.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.RTLD_NOW
import platform.posix.dlopen

@OptIn(ExperimentalForeignApi::class)
actual object NativeLibLoader {
    actual fun env(name: String) {
        dlopen(name, RTLD_NOW)
    }

    actual fun resource(name: String) {
        dlopen(name, RTLD_NOW)
    }
}