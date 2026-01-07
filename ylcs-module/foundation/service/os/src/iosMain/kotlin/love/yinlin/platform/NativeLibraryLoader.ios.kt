package love.yinlin.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.RTLD_NOW
import platform.posix.dlopen

@OptIn(ExperimentalForeignApi::class)
actual fun loadNativeLibrary(name: String) {
    dlopen(name, RTLD_NOW)
}