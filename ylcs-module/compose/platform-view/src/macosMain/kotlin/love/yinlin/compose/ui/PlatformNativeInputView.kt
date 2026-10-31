@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AppKit.NSView
import platform.Foundation.NSPoint
import platform.Foundation.NSRect

internal class PlatformNativeInputView(frame: CValue<NSRect>, private var hit: ((CValue<NSPoint>) -> NSView?)?) : NSView(frame) {
    override fun hitTest(point: CValue<NSPoint>): NSView? = hit?.invoke(point)

    fun dispose() { hit = null }
}
