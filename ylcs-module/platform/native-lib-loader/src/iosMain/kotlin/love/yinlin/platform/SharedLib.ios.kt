package love.yinlin.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.RTLD_NOW
import platform.posix.dlopen

@OptIn(ExperimentalForeignApi::class)
actual sealed interface SharedLib {
    actual fun load()

    actual class Env actual constructor(private val name: String) : SharedLib {
        actual override fun load() {
            dlopen(name, RTLD_NOW)
        }
    }

    actual class Resource actual constructor(private val name: String) : SharedLib {
        actual override fun load() {
            dlopen(name, RTLD_NOW)
        }
    }
}