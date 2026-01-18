package org.libpag

import love.yinlin.extension.NativeLib
import love.yinlin.platform.NativeLibLoader

@NativeLib
class PAGShapeLayer internal constructor(constructor: () -> Long) : PAGLayer(constructor, PAGShapeLayer::nativeRelease) {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeRelease(handle: Long)
    }
}