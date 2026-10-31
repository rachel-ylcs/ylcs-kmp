@file:OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
package love.yinlin.compose.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.cinterop.*
import platform.windows.*

internal object WindowsPlatformViewInput {
    private val containers: MutableSet<WindowsPlatformViewContainer<*>> = []
    private var hook: HHOOK? = null

    fun register(container: WindowsPlatformViewContainer<*>) {
        if (hook == null) hook = checkNotNull(SetWindowsHookExW(WH_MOUSE, staticCFunction(::platformViewMouseHook), null, GetCurrentThreadId())) { "Cannot register native view input: ${GetLastError()}" }
        containers.add(container)
    }

    fun unregister(container: WindowsPlatformViewContainer<*>) {
        containers.remove(container)
        if (containers.isEmpty()) {
            hook?.let(::UnhookWindowsHookEx)
            hook = null
        }
    }

    fun route(message: UInt, data: CPointer<MOUSEHOOKSTRUCT>): Boolean {
        val container = containers.firstOrNull { it.contains(data.pointed.hwnd) } ?: return false

        return try {
            val capture = GetCapture()
            if (capture != null && container.contains(capture)) return false
            memScoped {
                val point = alloc<POINT>()
                point.x = data.pointed.pt.x
                point.y = data.pointed.pt.y
                if (ScreenToClient(container.parent, point.ptr) == 0) return@memScoped false
                if (container.context.hitTestNativeView(point.x.toFloat(), point.y.toFloat()) != null) {
                    if (message == WM_LBUTTONDOWN.toUInt() || message == WM_RBUTTONDOWN.toUInt() || message == WM_MBUTTONDOWN.toUInt() || message == WM_XBUTTONDOWN.toUInt()) container.nativeFocus()
                    return@memScoped false
                }
                if (message !in WM_MOUSEFIRST.toUInt() .. WM_MOUSELAST.toUInt()) return@memScoped false
                val wheel = message == WM_MOUSEWHEEL.toUInt() || message == WM_MOUSEHWHEEL.toUInt()
                var buttons = 0
                if (GetKeyState(VK_LBUTTON).toInt() < 0) buttons = buttons or MK_LBUTTON
                if (GetKeyState(VK_RBUTTON).toInt() < 0) buttons = buttons or MK_RBUTTON
                if (GetKeyState(VK_MBUTTON).toInt() < 0) buttons = buttons or MK_MBUTTON
                if (GetKeyState(VK_XBUTTON1).toInt() < 0) buttons = buttons or MK_XBUTTON1
                if (GetKeyState(VK_XBUTTON2).toInt() < 0) buttons = buttons or MK_XBUTTON2
                if (GetKeyState(VK_SHIFT).toInt() < 0) buttons = buttons or MK_SHIFT
                if (GetKeyState(VK_CONTROL).toInt() < 0) buttons = buttons or MK_CONTROL
                val extra = if (wheel || message == WM_XBUTTONDOWN.toUInt() || message == WM_XBUTTONUP.toUInt() || message == WM_XBUTTONDBLCLK.toUInt()) data.reinterpret<MOUSEHOOKSTRUCTEX>().pointed.mouseData and 0xFFFF0000u else 0u
                val x = if (wheel) data.pointed.pt.x else point.x
                val y = if (wheel) data.pointed.pt.y else point.y
                val position = ((y.toLong() and 0xFFFFL) shl 16) or (x.toLong() and 0xFFFFL)
                if ((message == WM_LBUTTONDOWN.toUInt() || message == WM_RBUTTONDOWN.toUInt() || message == WM_MBUTTONDOWN.toUInt() || message == WM_XBUTTONDOWN.toUInt()) && GetWindowLongW(container.parent, GWL_EXSTYLE).toLong() and WS_EX_NOACTIVATE.toLong() == 0L) SetFocus(container.parent)
                SendMessageW(container.parent, message, buttons.toULong() or extra.toULong(), position)
                true
            }
        }
        catch (failure: Throwable) {
            container.reportFailure(failure)
            false
        }
    }
}

private fun platformViewMouseHook(code: Int, message: WPARAM, parameter: LPARAM): LRESULT {
    if (code == HC_ACTION) {
        val data = parameter.toCPointer<MOUSEHOOKSTRUCT>()
        if (data != null && WindowsPlatformViewInput.route(message.toUInt(), data)) return 1L
    }

    return CallNextHookEx(null, code, message, parameter)
}
