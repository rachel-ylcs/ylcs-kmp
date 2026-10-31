@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AppKit.NSView
import platform.Foundation.NSMakeRect
import platform.Foundation.NSPoint

internal class PlatformNativeContainer : NSView(NSMakeRect(0.0, 0.0, 0.0, 0.0)) {
    init {
        wantsLayer = true
        layer?.masksToBounds = true
        hidden = true
    }

    override fun isFlipped(): Boolean = true

    override fun hitTest(point: CValue<NSPoint>): NSView? = super.hitTest(point)?.takeUnless { it === this }
}
