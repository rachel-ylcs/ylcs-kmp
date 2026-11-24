package love.yinlin.foundation.os.battery

import kotlinx.cinterop.*
import platform.windows.FALSE
import platform.windows.GetSystemPowerStatus
import platform.windows.SYSTEM_POWER_STATUS

@OptIn(ExperimentalForeignApi::class)
internal object WindowsBatteryApi {
    fun readLevel(): BatteryLevel = memScoped {
        val status = alloc<SYSTEM_POWER_STATUS>()
        if (GetSystemPowerStatus(status.ptr) == FALSE) BatteryLevel.Unknown
        else BatteryLevel.fromWindows(status.BatteryFlag.toInt(), status.BatteryLifePercent.toInt())
    }
}
