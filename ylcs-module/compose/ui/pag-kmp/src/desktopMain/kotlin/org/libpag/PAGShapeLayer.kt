package org.libpag

import love.yinlin.annotation.NativeLibApi
import love.yinlin.platform.NativeLibLoader

@NativeLibApi
class PAGShapeLayer internal constructor(
    constructor: () -> Long
) : PAGLayer(PAGShapeLayer::nativeRelease, constructor) {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeRelease(handle: Long)
    }
}