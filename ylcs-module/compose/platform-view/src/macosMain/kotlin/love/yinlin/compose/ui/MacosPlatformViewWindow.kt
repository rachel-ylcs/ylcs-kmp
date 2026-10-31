@file:OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
package love.yinlin.compose.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.NativeWindowContext
import kotlinx.cinterop.*
import org.jetbrains.skiko.window.NativeWindowScope
import platform.AppKit.*
import platform.Foundation.NSPoint
import platform.QuartzCore.CAMetalLayer

internal class MacosPlatformViewWindow private constructor(private val context: NativeWindowContext) {
    val window: NSWindow
    val parent: NSView
    val compositor: NSView
    private val metalLayer: CAMetalLayer
    private val originalOpaque: Boolean
    private val inputs: PlatformNativeInputView
    private val views: MutableSet<MacosPlatformViewContainer<*>> = []
    private var failure: Throwable? = null

    init {
        val handle = checkNotNull(context.window.windowHandle?.toCPointer<CPointed>()) { "The Compose window has no NSWindow" }
        window = interpretObjCPointer(handle.rawValue)
        parent = checkNotNull(window.contentView) { "The Compose window has no content view" }
        compositor = checkNotNull(parent.subviews.filterIsInstance<NSView>().lastOrNull { it.layer is CAMetalLayer }) { "Native views require the Native Compose Metal renderer" }
        metalLayer = compositor.layer as CAMetalLayer
        originalOpaque = metalLayer.opaque
        metalLayer.opaque = false

        try {
            inputs = PlatformNativeInputView(parent.bounds, ::hitTest).apply {
                autoresizingMask = NSViewWidthSizable or NSViewHeightSizable
            }
            parent.addSubview(inputs)
        }
        catch (failure: Throwable) {
            metalLayer.opaque = originalOpaque
            throw failure
        }
    }

    fun add(view: MacosPlatformViewContainer<*>) { views.add(view) }

    fun remove(view: MacosPlatformViewContainer<*>) {
        views.remove(view)
        if (views.isEmpty()) {
            inputs.dispose()
            inputs.removeFromSuperview()
            metalLayer.opaque = originalOpaque
            windows.remove(context.window)
        }
    }

    fun checkFailure() { failure?.let { throw it } }

    private fun hitTest(point: CValue<NSPoint>): NSView? {
        return try {
            val bounds = parent.bounds
            val scale = window.backingScaleFactor
            val x = point.useContents { x } - bounds.useContents { origin.x }
            val y = point.useContents { y } - bounds.useContents { origin.y }
            val top = if (parent.isFlipped()) y else bounds.useContents { size.height } - y
            val handle = context.hitTestNativeView((x * scale).toFloat(), (top * scale).toFloat())?.nativeHandle
            val view = views.firstOrNull { it.nativeHandle == handle } ?: return parent
            val target = view.hitTest(point) ?: return parent
            val type = NSApplication.sharedApplication.currentEvent?.type
            if (type == NSEventTypeLeftMouseDown || type == NSEventTypeRightMouseDown || type == NSEventTypeOtherMouseDown) view.nativeFocus()
            target
        }
        catch (error: Throwable) {
            failure = error
            try { context.window.invalidate() }
            catch (cleanup: Throwable) { if (cleanup !== error) error.addSuppressed(cleanup) }
            parent
        }
    }

    companion object {
        private val windows = mutableMapOf<NativeWindowScope, MacosPlatformViewWindow>()

        fun acquire(context: NativeWindowContext): MacosPlatformViewWindow = windows.getOrPut(context.window) { MacosPlatformViewWindow(context) }
    }
}
