package love.yinlin

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT
import love.yinlin.platform.OS
import love.yinlin.platform.Platform
import kotlin.system.exitProcess

object SingleInstance {
    fun check() {
        val exists = when (OS.platform) {
            Platform.Windows -> {
                val instance = Kernel32.INSTANCE
                val name = "ylcs-desktop"
                if (instance.OpenEvent(WinNT.EVENT_ALL_ACCESS, false, name) != null) true
                else {
                    instance.CreateEvent(null, false, false, name)
                    false
                }
            }
            Platform.Linux -> {
                // TODO:
                false
            }
            Platform.MacOS -> {
                // TODO:
                false
            }
            else -> false
        }
        if (exists) exitProcess(0)
    }
}