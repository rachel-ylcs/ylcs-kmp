@file:OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
package love.yinlin.compose.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.window.NativeWindowContext
import kotlinx.cinterop.*
import platform.AppKit.NSView
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowBelow
import platform.Foundation.NSMakeRect
import platform.Foundation.NSPoint
import platform.Foundation.NSThread

internal class MacosPlatformViewContainer<V : NSView>(context: NativeWindowContext, private val onNativeFocus: () -> Unit, private val factory: (NSView) -> V) : NativePlatformViewContainer<V> {
    private val host: MacosPlatformViewWindow
    private val nativeWindow: NSWindow
    private val parent: NSView
    private var container: PlatformNativeContainer? = null
    private var view: V? = null
    private var previousBounds: Rect? = null
    private var previousVisible: Rect? = null
    private var previousScale = 0.0

    override val nativeHandle: Long get() = container?.objcPtr()?.toLong() ?: 0L

    override val hasFocus: Boolean get() {
        host.checkFailure()
        val container = container ?: return false
        val responder = nativeWindow.firstResponder as? NSView ?: return false
        return nativeWindow.isKeyWindow() && responder.isDescendantOf(container)
    }

    init {
        check(NSThread.isMainThread) { "Create platform views on the AppKit main thread" }
        host = MacosPlatformViewWindow.acquire(context)
        nativeWindow = host.window
        parent = host.parent
        try {
            container = PlatformNativeContainer().also { parent.addSubview(it, NSWindowBelow, host.compositor) }
            host.add(this)
        }
        catch (failure: Throwable) {
            container?.removeFromSuperview()
            host.remove(this)
            throw failure
        }
    }

    override fun bringToFront() {
        container?.let { parent.addSubview(it, NSWindowBelow, host.compositor) }
    }

    fun hitTest(point: CValue<NSPoint>): NSView? = container?.hitTest(point)

    fun nativeFocus() = onNativeFocus()

    override fun create(): V {
        val container = checkNotNull(container)
        val created = factory(container)
        check(created.superview == null) { "The platform factory must return an unattached NSView" }
        view = created
        container.addSubview(created)
        return created
    }

    override fun place(bounds: Rect, visibleBounds: Rect) {
        val container = container ?: return
        val view = view ?: return
        val scale = nativeWindow.backingScaleFactor
        if (bounds == previousBounds && visibleBounds == previousVisible && scale == previousScale) return
        val parentBounds = parent.bounds
        val x = parentBounds.useContents { origin.x } + visibleBounds.left / scale
        val y = parentBounds.useContents { origin.y + if (parent.isFlipped()) visibleBounds.top / scale else size.height - visibleBounds.bottom / scale }
        container.setFrame(NSMakeRect(x, y, visibleBounds.width / scale, visibleBounds.height / scale))
        view.setFrame(NSMakeRect((bounds.left - visibleBounds.left) / scale, (bounds.top - visibleBounds.top) / scale, bounds.width / scale, bounds.height / scale))
        container.hidden = false
        previousBounds = bounds
        previousVisible = visibleBounds
        previousScale = scale
    }

    override fun hide() {
        previousBounds = null
        previousVisible = null
        container?.hidden = true
    }

    override fun close() {
        val previous = container ?: return
        val previousView = view
        container = null
        view = null
        previousBounds = null
        previousVisible = null
        val responder = nativeWindow.firstResponder as? NSView
        if (responder != null && responder.isDescendantOf(previous)) nativeWindow.makeFirstResponder(parent)

        platformViewCleanup(
            { previous.hidden = true },
            { previousView?.removeFromSuperview() },
            previous::removeFromSuperview,
            { host.remove(this) }
        )
    }
}
