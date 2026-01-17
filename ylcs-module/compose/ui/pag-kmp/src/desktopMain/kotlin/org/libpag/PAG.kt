package org.libpag

import love.yinlin.extension.NativeLib
import love.yinlin.platform.NativeLibLoader

@NativeLib
object PAG {
    init {
        NativeLibLoader.resource("pag_kmp")
    }

    @JvmStatic
    private external fun nativeSDKVersion(): String

    val sdkVersion: String get() = nativeSDKVersion()
}