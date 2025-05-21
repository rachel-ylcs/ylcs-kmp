package love.yinlin

import androidx.compose.runtime.Stable
import androidx.compose.ui.awt.ComposeWindow
import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import love.yinlin.platform.OS
import love.yinlin.platform.Platform

@Stable
object TransparentWindow {
    fun run(window: ComposeWindow) {
        when (OS.platform) {
            Platform.Windows -> {
                val hwnd = WinDef.HWND(Native.getComponentPointer(window))
                val exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE) or WinUser.WS_EX_LAYERED or WinUser.WS_EX_TRANSPARENT
                User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle)
            }
            Platform.Linux -> {

            }
            Platform.MacOS -> {

            }
            else -> {}
        }
    }
}