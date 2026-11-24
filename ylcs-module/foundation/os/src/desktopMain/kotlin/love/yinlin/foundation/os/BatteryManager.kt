package love.yinlin.foundation.os

import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.PlatformContext
import love.yinlin.platform.Platform
import love.yinlin.platform.ffi.*
import love.yinlin.platform.platform
import java.lang.foreign.ValueLayout

actual object BatteryManager {
    private object Windows : Kernel32Library() {
        val GetSystemPowerStatus by func(NativeType.Pointer, retType = Win32Type.BOOL)

        fun readLevel(): BatteryLevel = useMemory { arena ->
            // SYSTEM_POWER_STATUS: 四个 BYTE 字段及两个 DWORD 字段。
            val status = arena.allocate(12, 4)
            if (GetSystemPowerStatus(status) as Int == Win32.FALSE) BatteryLevel.Unknown
            else BatteryLevel.fromWindows(
                status.get(ValueLayout.JAVA_BYTE, 1).toUByte().toInt(),
                status.get(ValueLayout.JAVA_BYTE, 2).toUByte().toInt(),
            )
        }
    }

    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = Coroutines.io {
        catchingDefault<BatteryLevel>(BatteryLevel.Unknown) {
            when (platform) {
                Platform.Windows -> Windows.readLevel()
                Platform.Linux -> LinuxBatteryReader.readLevel()
                Platform.MacOS -> MacBatteryApi.readLevel()
                else -> BatteryLevel.Unsupported
            }
        }
    }
}
