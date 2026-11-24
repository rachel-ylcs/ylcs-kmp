package love.yinlin.foundation.os.battery

import love.yinlin.platform.ffi.*
import java.lang.foreign.ValueLayout

internal object WindowsBatteryApi : Kernel32Library() {
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
