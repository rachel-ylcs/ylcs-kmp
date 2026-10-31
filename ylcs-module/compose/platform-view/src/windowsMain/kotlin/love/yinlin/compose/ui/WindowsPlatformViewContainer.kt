@file:OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
package love.yinlin.compose.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import kotlinx.cinterop.*
import androidx.compose.ui.window.NativeWindowContext
import platform.windows.*
import kotlin.math.ceil
import kotlin.math.floor

internal class WindowsPlatformViewContainer<V : COpaquePointer>(
    val context: NativeWindowContext,
    private val onNativeFocus: () -> Unit,
    private val factory: (HWND) -> V
) : NativePlatformViewContainer<V> {
    val parent = checkNotNull(context.window.windowHandle?.toCPointer<HWND__>()) { "The Compose window has no HWND" }
    private var container: HWND? = null
    private var view: HWND? = null
    private var previousBounds: Rect? = null
    private var previousVisible: Rect? = null
    private var callbackFailure: Throwable? = null

    override val nativeHandle: Long get() = container?.rawValue?.toLong() ?: 0L

    override val hasFocus: Boolean get() {
        callbackFailure?.let { throw it }
        val container = container ?: return false
        val focused = GetFocus() ?: return false
        return focused == container || IsChild(container, focused) != 0
    }

    init {
        check(IsWindow(parent) != 0 && GetWindowThreadProcessId(parent, null) == GetCurrentThreadId()) { "Create platform views on their window's UI thread" }
        container = checkNotNull(CreateWindowExW(WS_EX_CONTROLPARENT.toUInt(), "STATIC", "", (WS_CHILD or WS_CLIPCHILDREN or WS_CLIPSIBLINGS).toUInt(), 0, 0, 0, 0, parent, null, GetModuleHandleW(null), null)) { "Cannot create the platform view container: ${GetLastError()}" }

        try { WindowsPlatformViewInput.register(this) }
        catch (failure: Throwable) {
            close()
            throw failure
        }
    }

    override fun create(): V {
        val parent = checkNotNull(container)
        val created = factory(parent)
        val handle = created.reinterpret<HWND__>()
        check(IsWindow(handle) != 0 && GetParent(handle) == parent && IsChild(parent, handle) != 0 && GetWindowThreadProcessId(handle, null) == GetCurrentThreadId()) { "The platform factory must create a child HWND using environment.parentWindow" }
        view = handle
        ShowWindow(handle, SW_SHOWNA)
        return created
    }

    override fun place(bounds: Rect, visibleBounds: Rect) {
        val container = container ?: return
        val view = view ?: return
        if (bounds == previousBounds && visibleBounds == previousVisible) return
        val left = floor(visibleBounds.left).toInt()
        val top = floor(visibleBounds.top).toInt()
        val right = ceil(visibleBounds.right).toInt()
        val bottom = ceil(visibleBounds.bottom).toInt()
        val x = floor(bounds.left).toInt() - left
        val y = floor(bounds.top).toInt() - top
        val width = ceil(bounds.right).toInt() - floor(bounds.left).toInt()
        val height = ceil(bounds.bottom).toInt() - floor(bounds.top).toInt()
        check(SetWindowPos(view, null, x, y, width, height, (SWP_NOZORDER or SWP_NOACTIVATE).toUInt()) != 0) { "Cannot lay out the platform HWND: ${GetLastError()}" }
        check(SetWindowPos(container, null, left, top, right - left, bottom - top, (SWP_NOZORDER or SWP_NOACTIVATE or SWP_SHOWWINDOW).toUInt()) != 0) { "Cannot lay out the platform container: ${GetLastError()}" }
        previousBounds = bounds
        previousVisible = visibleBounds
    }

    override fun bringToFront() {
        container?.let {
            SetWindowPos(it, HWND_TOP, 0, 0, 0, 0, (SWP_NOACTIVATE or SWP_NOMOVE or SWP_NOSIZE).toUInt())
        }
    }

    fun contains(handle: HWND?): Boolean {
        val container = container ?: return false
        return handle != null && (handle == container || IsChild(container, handle) != 0)
    }

    fun nativeFocus() = onNativeFocus()

    fun reportFailure(failure: Throwable) {
        callbackFailure = failure
        try {
            context.window.invalidate()
        }
        catch (cleanup: Throwable) {
            if (cleanup !== failure) failure.addSuppressed(cleanup)
        }
    }

    override fun hide() {
        previousBounds = null
        previousVisible = null
        container?.let { if (IsWindow(it) != 0) ShowWindow(it, SW_HIDE) }
    }

    override fun close() {
        WindowsPlatformViewInput.unregister(this)
        val previous = container
        container = null
        view = null
        previousBounds = null
        previousVisible = null

        if (previous != null && IsWindow(previous) != 0) {
            val focused = GetFocus()
            if (focused == previous || focused != null && IsChild(previous, focused) != 0) SetFocus(parent)
            DestroyWindow(previous)
        }
    }
}
