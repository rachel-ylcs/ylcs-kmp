package love.yinlin.foundation.os

import kotlinx.cinterop.*
import love.yinlin.foundation.PlatformContext
import platform.windows.FALSE
import platform.windows.GetSystemPowerStatus
import platform.windows.SYSTEM_POWER_STATUS

@OptIn(ExperimentalForeignApi::class)
actual object BatteryManager {
    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = memScoped {
        val status = alloc<SYSTEM_POWER_STATUS>()
        if (GetSystemPowerStatus(status.ptr) == FALSE) BatteryLevel.Unknown
        else BatteryLevel.fromWindows(status.BatteryFlag.toInt(), status.BatteryLifePercent.toInt())
    }
}
