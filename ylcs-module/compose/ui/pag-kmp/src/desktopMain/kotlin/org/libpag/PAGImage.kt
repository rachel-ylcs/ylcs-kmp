package org.libpag

import love.yinlin.extension.NativeLib
import love.yinlin.platform.NativeLibLoader

@NativeLib
class PAGImage {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }
    }
}