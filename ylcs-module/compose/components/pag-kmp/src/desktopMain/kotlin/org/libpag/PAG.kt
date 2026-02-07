package org.libpag

import love.yinlin.annotation.NativeLibApi
import love.yinlin.platform.NativeLibLoader

@NativeLibApi
object PAG {
    init {
        NativeLibLoader.resource("pag_kmp")
    }

    @JvmStatic
    private external fun nativeSDKVersion(): String

    val sdkVersion: String get() = nativeSDKVersion()
}