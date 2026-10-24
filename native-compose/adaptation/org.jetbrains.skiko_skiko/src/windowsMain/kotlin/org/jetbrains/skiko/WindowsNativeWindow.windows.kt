@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.concurrent.atomics.ExperimentalAtomicApi::class,
)

package org.jetbrains.skiko

import kotlinx.cinterop.alloc
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.convert
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.wcstr
import org.jetbrains.skia.impl.NativePointer
import platform.windows.BeginPaint
import platform.windows.BI_RGB
import platform.windows.BITMAPINFO
import platform.windows.BITMAPINFOHEADER
import platform.windows.CS_HREDRAW
import platform.windows.CS_OWNDC
import platform.windows.CS_VREDRAW
import platform.windows.CW_USEDEFAULT
import platform.windows.CallWindowProcW
import platform.windows.CreateWindowExW
import platform.windows.DIB_RGB_COLORS
import platform.windows.DefWindowProcW
import platform.windows.DestroyWindow
import platform.windows.DispatchMessageW
import platform.windows.EndPaint
import platform.windows.GetClientRect
import platform.windows.GetDC
import platform.windows.GetDeviceCaps
import platform.windows.GetLastError
import platform.windows.GetModuleHandleW
import platform.windows.GetWindowLongPtrW
import platform.windows.GDI_ERROR
import platform.windows.GWLP_WNDPROC
import platform.windows.HWND
import platform.windows.HWND__
import platform.windows.IsWindow
import platform.windows.LOGPIXELSX
import platform.windows.LPARAM
import platform.windows.LRESULT
import platform.windows.MSG
import platform.windows.PAINTSTRUCT
import platform.windows.PeekMessageW
import platform.windows.PM_REMOVE
import platform.windows.PostMessageW
import platform.windows.RECT
import platform.windows.RegisterClassExW
import platform.windows.RegisterWindowMessageW
import platform.windows.ReleaseDC
import platform.windows.SetWindowPos
import platform.windows.SetLastError
import platform.windows.SetWindowLongPtrW
import platform.windows.ShowWindow
import platform.windows.SRCCOPY
import platform.windows.StretchDIBits
import platform.windows.SWP_NOACTIVATE
import platform.windows.SWP_NOZORDER
import platform.windows.SW_SHOW
import platform.windows.TranslateMessage
import platform.windows.UINT
import platform.windows.UpdateWindow
import platform.windows.VREFRESH
import platform.windows.WM_APP
import platform.windows.WM_CLOSE
import platform.windows.WM_DESTROY
import platform.windows.WM_DISPLAYCHANGE
import platform.windows.WM_DPICHANGED
import platform.windows.WM_ERASEBKGND
import platform.windows.WM_ENTERSIZEMOVE
import platform.windows.WM_EXITSIZEMOVE
import platform.windows.WM_PAINT
import platform.windows.WM_NCDESTROY
import platform.windows.WM_QUIT
import platform.windows.WM_SIZE
import platform.windows.WNDCLASSEXW
import platform.windows.WPARAM
import platform.windows.WS_OVERLAPPEDWINDOW
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.native.internal.NativePtr

private const val SKIKO_WINDOW_CLASS = "SkikoKotlinNativeWindow"
private val WM_SKIKO_RENDER =
    RegisterWindowMessageW("org.jetbrains.skiko.WindowsNativeWindow.Render")
        .takeIf { it != 0u }
        ?: (WM_APP.toUInt() + 0x034Bu)
private val windowsByHandle = mutableMapOf<Long, WindowsNativeWindow>()
private var windowClassRegistered = false
private val skikoWindowProcPointer = staticCFunction(::skikoWindowProc)

/**
 * A Win32 window accepted by [SkiaLayer.attachTo].
 *
 * Construct it with an existing HWND encoded as a [Long], or use [create] to create a Skiko-owned
 * top-level window with built-in paint, resize, and asynchronous render dispatch.
 */
class WindowsNativeWindow private constructor(
    val handle: Long,
    private val owned: Boolean,
) {
    constructor(handle: Long) : this(handle, owned = false)

    init {
        require(handle != 0L) { "A null HWND cannot host a SkiaLayer" }
    }

    internal var paintCallback: (() -> Unit)? = null
    internal var renderCallback: (() -> Unit)? = null
    internal var destroyCallback: (() -> Unit)? = null
    internal var callbackFailureHandler: ((Throwable) -> Unit)? = null
    private val renderMessageQueued = AtomicBoolean(false)
    internal var isLiveResize = false
        private set
    private var previousWindowProc: platform.windows.WNDPROC? = null
    private var dispatchInstalled = false

    internal val isOwnedBySkiko: Boolean
        get() = owned

    val isValid: Boolean
        get() = IsWindow(hwnd()) != 0

    internal val clientSize: Pair<Int, Int>
        get() = memScoped {
            val rect = alloc<RECT>()
            if (GetClientRect(hwnd(), rect.ptr) == 0) return@memScoped 0 to 0
            (rect.right - rect.left) to (rect.bottom - rect.top)
        }

    internal val contentScale: Float
        get() {
            val dc = GetDC(hwnd()) ?: return 1f
            return try {
                GetDeviceCaps(dc, LOGPIXELSX).coerceAtLeast(96) / 96f
            } finally {
                ReleaseDC(hwnd(), dc)
            }
        }

    internal val displayRefreshRate: Float
        get() {
            val dc = GetDC(hwnd()) ?: return 60f
            return try {
                GetDeviceCaps(dc, VREFRESH).takeIf { it > 1 }?.toFloat() ?: 60f
            } finally {
                ReleaseDC(hwnd(), dc)
            }
        }

    fun show() {
        check(isValid) { "The HWND is no longer valid" }
        ShowWindow(hwnd(), SW_SHOW)
        UpdateWindow(hwnd())
    }

    /** Processes all currently queued messages. Returns false after WM_QUIT. */
    fun pumpMessages(): Boolean = memScoped {
        val message = alloc<MSG>()
        while (PeekMessageW(message.ptr, null, 0u, 0u, PM_REMOVE.toUInt()) != 0) {
            if (message.message == WM_QUIT.toUInt()) return@memScoped false
            TranslateMessage(message.ptr)
            DispatchMessageW(message.ptr)
        }
        true
    }

    internal fun requestRender() {
        if (!isValid) return
        if (!renderMessageQueued.compareAndSet(expectedValue = false, newValue = true)) return
        if (PostMessageW(hwnd(), WM_SKIKO_RENDER, 0u, 0L) == 0) {
            renderMessageQueued.store(false)
            reportCallbackFailure(
                RenderException("PostMessageW failed (Win32 error ${GetLastError()})"),
            )
        }
    }

    internal fun attachCallbacks(
        paint: () -> Unit,
        render: () -> Unit,
        onDestroyed: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        check(paintCallback == null && renderCallback == null) {
            "This WindowsNativeWindow is already attached to a SkiaLayer"
        }
        ensureDispatchInstalled()
        paintCallback = paint
        renderCallback = render
        destroyCallback = onDestroyed
        callbackFailureHandler = onFailure
    }

    internal fun detachCallbacks() {
        paintCallback = null
        renderCallback = null
        destroyCallback = null
        renderMessageQueued.store(false)
        isLiveResize = false
        if (!owned) uninstallExternalDispatch()
        callbackFailureHandler = null
    }

    internal fun dispatchRenderMessage() {
        renderMessageQueued.store(false)
        invokeCallback(renderCallback)
    }

    internal fun dispatchPaintMessage() {
        invokeCallback(paintCallback)
    }

    internal fun beginLiveResize() {
        isLiveResize = true
    }

    internal fun endLiveResize() {
        if (!isLiveResize) return
        isLiveResize = false
        // Request one frame at the final client size. The Direct3D renderer defers its expensive
        // swap-chain resize until this point.
        dispatchPaintMessage()
    }

    internal fun forwardMessage(
        hwnd: HWND,
        message: UINT,
        wParam: WPARAM,
        lParam: LPARAM,
    ): LRESULT =
        previousWindowProc?.let { CallWindowProcW(it, hwnd, message, wParam, lParam) }
            ?: DefWindowProcW(hwnd, message, wParam, lParam)

    internal fun handleNativeDestroy() {
        invokeCallback(destroyCallback)
        paintCallback = null
        renderCallback = null
        destroyCallback = null
        callbackFailureHandler = null
        renderMessageQueued.store(false)
        isLiveResize = false
    }

    internal fun handleNativeFinalDestroy() {
        handleNativeDestroy()
        windowsByHandle.remove(handle)
        previousWindowProc = null
        dispatchInstalled = false
    }

    internal fun reportWindowProcedureFailure(failure: Throwable) {
        reportCallbackFailure(failure)
    }

    private fun invokeCallback(callback: (() -> Unit)?) {
        try {
            callback?.invoke()
        } catch (failure: Throwable) {
            reportCallbackFailure(failure)
        }
    }

    private fun reportCallbackFailure(failure: Throwable) {
        try {
            callbackFailureHandler?.invoke(failure)
        } catch (_: Throwable) {
            // A Kotlin exception must never unwind through the Win32 WNDPROC ABI boundary.
        }
    }

    private fun ensureDispatchInstalled() {
        if (dispatchInstalled) return
        if (owned) {
            check(windowsByHandle[handle] === this) { "Owned HWND is not registered" }
            dispatchInstalled = true
            return
        }
        check(windowsByHandle[handle] == null) {
            "HWND $handle is already wrapped by another WindowsNativeWindow"
        }
        SetLastError(0u)
        val previousAddress =
            SetWindowLongPtrW(hwnd(), GWLP_WNDPROC, skikoWindowProcPointer.rawValue.toLong())
        val error = GetLastError()
        if (previousAddress == 0L && error != 0u) {
            throw RenderException("Could not subclass HWND $handle (Win32 error $error)")
        }
        previousWindowProc =
            interpretCPointer<CFunction<(HWND?, UINT, WPARAM, LPARAM) -> LRESULT>>(
                NativePtr.NULL + previousAddress,
            ) ?: throw RenderException("HWND $handle had a null window procedure")
        windowsByHandle[handle] = this
        dispatchInstalled = true
    }

    private fun uninstallExternalDispatch() {
        if (!dispatchInstalled || owned) return
        val previous = previousWindowProc ?: return
        if (isValid) {
            val currentAddress = GetWindowLongPtrW(hwnd(), GWLP_WNDPROC)
            if (currentAddress != skikoWindowProcPointer.rawValue.toLong()) {
                // Another subclass currently sits above Skiko and still forwards to this WNDPROC.
                return
            }
            SetLastError(0u)
            val replacedAddress =
                SetWindowLongPtrW(hwnd(), GWLP_WNDPROC, previous.rawValue.toLong())
            val error = GetLastError()
            if (replacedAddress == 0L && error != 0u) {
                reportCallbackFailure(
                    RenderException("Could not restore HWND $handle WNDPROC (Win32 error $error)"),
                )
                return
            }
        }
        windowsByHandle.remove(handle)
        previousWindowProc = null
        dispatchInstalled = false
    }

    internal fun presentSoftwareFrame(
        pixels: NativePointer,
        width: Int,
        height: Int,
        rowBytes: Int,
    ) {
        require(width > 0 && height > 0 && rowBytes == width * 4)
        memScoped {
            val bitmapInfo = alloc<BITMAPINFO>()
            bitmapInfo.bmiHeader.biSize = sizeOf<BITMAPINFOHEADER>().convert()
            bitmapInfo.bmiHeader.biWidth = width
            bitmapInfo.bmiHeader.biHeight = -height
            bitmapInfo.bmiHeader.biPlanes = 1.convert()
            bitmapInfo.bmiHeader.biBitCount = 32.convert()
            bitmapInfo.bmiHeader.biCompression = BI_RGB.convert()

            val dc = GetDC(hwnd())
                ?: throw RenderException("GetDC failed (Win32 error ${GetLastError()})")
            try {
                val result =
                    StretchDIBits(
                        dc,
                        0,
                        0,
                        width,
                        height,
                        0,
                        0,
                        width,
                        height,
                        interpretCPointer<ByteVar>(pixels),
                        bitmapInfo.ptr,
                        DIB_RGB_COLORS.convert(),
                        SRCCOPY.convert(),
                    )
                if (result == 0 || result == GDI_ERROR.toInt()) {
                    throw RenderException("StretchDIBits failed (Win32 error ${GetLastError()})")
                }
            } finally {
                ReleaseDC(hwnd(), dc)
            }
        }
    }

    fun close() {
        if (owned && isValid) DestroyWindow(hwnd()) else detachCallbacks()
    }

    internal fun hwnd(): HWND =
        interpretCPointer<HWND__>(NativePtr.NULL + handle) ?: error("Invalid HWND: $handle")

    companion object {
        fun create(
            title: String = "Skiko",
            width: Int = 800,
            height: Int = 600,
        ): WindowsNativeWindow {
            require(width > 0 && height > 0) { "Window dimensions must be positive" }
            registerWindowClass()
            val hwnd =
                CreateWindowExW(
                    0u,
                    SKIKO_WINDOW_CLASS,
                    title,
                    WS_OVERLAPPEDWINDOW.toUInt(),
                    CW_USEDEFAULT,
                    CW_USEDEFAULT,
                    width,
                    height,
                    null,
                    null,
                    GetModuleHandleW(null),
                    null,
                ) ?: error("CreateWindowExW failed")
            return WindowsNativeWindow(hwnd.rawValue.toLong(), owned = true).also { window ->
                windowsByHandle[window.handle] = window
                window.dispatchInstalled = true
            }
        }
    }
}

private fun registerWindowClass() {
    if (windowClassRegistered) return
    memScoped {
        val windowClass = alloc<WNDCLASSEXW>()
        windowClass.cbSize = sizeOf<WNDCLASSEXW>().convert()
        windowClass.style = (CS_HREDRAW or CS_VREDRAW or CS_OWNDC).convert()
        windowClass.lpfnWndProc = skikoWindowProcPointer
        windowClass.hInstance = GetModuleHandleW(null)
        windowClass.lpszClassName = SKIKO_WINDOW_CLASS.wcstr.ptr
        check(RegisterClassExW(windowClass.ptr) != 0.toUShort()) { "RegisterClassExW failed" }
    }
    windowClassRegistered = true
}

private fun skikoWindowProc(hwnd: HWND?, message: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
    if (hwnd == null) return 0L
    val handle = hwnd.rawValue.toLong()
    val window = windowsByHandle[handle]
    return try {
        when (message) {
            WM_SKIKO_RENDER -> {
                window?.dispatchRenderMessage()
                0L
            }
            WM_PAINT.toUInt() -> {
                if (window == null) {
                    DefWindowProcW(hwnd, message, wParam, lParam)
                } else if (window.isOwnedBySkiko) {
                    memScoped {
                        val paint = alloc<PAINTSTRUCT>()
                        BeginPaint(hwnd, paint.ptr)
                        try {
                            window.dispatchPaintMessage()
                        } finally {
                            EndPaint(hwnd, paint.ptr)
                        }
                    }
                    0L
                } else {
                    window.dispatchPaintMessage()
                    window.forwardMessage(hwnd, message, wParam, lParam)
                }
            }
            WM_ENTERSIZEMOVE.toUInt() -> {
                window?.beginLiveResize()
                window?.forwardMessage(hwnd, message, wParam, lParam)
                    ?: DefWindowProcW(hwnd, message, wParam, lParam)
            }
            WM_EXITSIZEMOVE.toUInt() -> {
                window?.endLiveResize()
                window?.forwardMessage(hwnd, message, wParam, lParam)
                    ?: DefWindowProcW(hwnd, message, wParam, lParam)
            }
            WM_SIZE.toUInt(), WM_DISPLAYCHANGE.toUInt() -> {
                window?.dispatchPaintMessage()
                window?.forwardMessage(hwnd, message, wParam, lParam)
                    ?: DefWindowProcW(hwnd, message, wParam, lParam)
            }
            WM_DPICHANGED.toUInt() -> {
                if (window?.isOwnedBySkiko == true) {
                    val suggested = interpretCPointer<RECT>(NativePtr.NULL + lParam)
                    suggested?.pointed?.let { rect ->
                        SetWindowPos(
                            hwnd,
                            null,
                            rect.left,
                            rect.top,
                            rect.right - rect.left,
                            rect.bottom - rect.top,
                            (SWP_NOZORDER or SWP_NOACTIVATE).convert(),
                        )
                    }
                }
                window?.dispatchPaintMessage()
                window?.forwardMessage(hwnd, message, wParam, lParam)
                    ?: DefWindowProcW(hwnd, message, wParam, lParam)
            }
            WM_ERASEBKGND.toUInt() ->
                if (window?.isOwnedBySkiko == true) 1L
                else window?.forwardMessage(hwnd, message, wParam, lParam)
                    ?: DefWindowProcW(hwnd, message, wParam, lParam)
            WM_CLOSE.toUInt() -> {
                if (window?.isOwnedBySkiko == true) {
                    DestroyWindow(hwnd)
                    0L
                } else {
                    window?.forwardMessage(hwnd, message, wParam, lParam)
                        ?: DefWindowProcW(hwnd, message, wParam, lParam)
                }
            }
            WM_DESTROY.toUInt() -> {
                val result =
                    window?.forwardMessage(hwnd, message, wParam, lParam)
                        ?: DefWindowProcW(hwnd, message, wParam, lParam)
                window?.handleNativeDestroy()
                result
            }
            WM_NCDESTROY.toUInt() -> {
                val result =
                    window?.forwardMessage(hwnd, message, wParam, lParam)
                        ?: DefWindowProcW(hwnd, message, wParam, lParam)
                window?.handleNativeFinalDestroy()
                result
            }
            else ->
                window?.forwardMessage(hwnd, message, wParam, lParam)
                    ?: DefWindowProcW(hwnd, message, wParam, lParam)
        }
    } catch (failure: Throwable) {
        window?.reportWindowProcedureFailure(failure)
        if (message == WM_SKIKO_RENDER) 0L
        else window?.forwardMessage(hwnd, message, wParam, lParam)
            ?: DefWindowProcW(hwnd, message, wParam, lParam)
    }
}
