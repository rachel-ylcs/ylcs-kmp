package org.libpag

import love.yinlin.extension.Destructible
import love.yinlin.extension.NativeLib
import love.yinlin.extension.RAII
import love.yinlin.platform.NativeLibLoader

@NativeLib
class PAGImage private constructor(constructor: () -> Long) : Destructible(RAII(constructor, ::nativeRelease)) {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeLoadFromPath(path: String): Long
        @JvmStatic
        private external fun nativeClear(handle: Long)
        @JvmStatic
        private external fun nativeRelease(handle: Long)

        fun loadFromPath(path: String): PAGImage = PAGImage { nativeLoadFromPath(path) }
    }

    fun clear() = nativeClear(nativeHandle)
}