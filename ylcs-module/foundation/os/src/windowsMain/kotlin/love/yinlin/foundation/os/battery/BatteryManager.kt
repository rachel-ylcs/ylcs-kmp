package love.yinlin.foundation.os.battery

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.PlatformContext
import platform.windows.FALSE
import platform.windows.GetSystemPowerStatus
import platform.windows.SYSTEM_POWER_STATUS

@OptIn(ExperimentalForeignApi::class)
actual object BatteryManager {
    actual suspend fun getBatteryInfo(context: PlatformContext): BatteryInfo = Coroutines.io {
        memScoped {
            val status = alloc<SYSTEM_POWER_STATUS>()
            if (GetSystemPowerStatus(status.ptr) == FALSE) BatteryInfo()
            else WindowsBatteryReader.readInfo(
                status.ACLineStatus.toInt(),
                status.BatteryFlag.toInt(),
                status.BatteryLifePercent.toInt(),
                status.Reserved1.toInt(), // Windows 10 用 SystemStatusFlag, 但 MinGW 仍然绑定了旧名称
                status.BatteryLifeTime,
            )
        }
    }
}
