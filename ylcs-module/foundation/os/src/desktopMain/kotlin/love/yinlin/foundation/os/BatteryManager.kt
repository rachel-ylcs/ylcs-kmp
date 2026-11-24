package love.yinlin.foundation.os

import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.PlatformContext
import love.yinlin.platform.Platform
import love.yinlin.platform.ffi.*
import love.yinlin.platform.platform
import java.lang.foreign.ValueLayout
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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

    private object MacOS {
        private val percentPattern = Regex("""(\d+)%""")

        fun readLevel(): BatteryLevel {
            val process = ProcessBuilder("/usr/bin/pmset", "-g", "batt").redirectErrorStream(true).start()
            try {
                if (!process.waitFor(2, TimeUnit.SECONDS) || process.exitValue() != 0) return BatteryLevel.Unknown
                val lines = process.inputStream.bufferedReader().use { it.readLines() }
                if (lines.firstOrNull()?.startsWith("Now drawing from") != true) return BatteryLevel.Unknown
                var total = 0
                var count = 0
                for (line in lines) {
                    if (!line.contains("InternalBattery") || line.contains("present: false")) continue
                    val percent = percentPattern.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return BatteryLevel.Unknown
                    if (percent !in 0 .. 100) return BatteryLevel.Unknown
                    total += percent
                    count++
                }
                return if (count == 0) BatteryLevel.NoBattery else BatteryLevel.Value((total.toDouble() / count).roundToInt())
            }
            finally {
                process.destroyForcibly()
                process.inputStream.close()
                process.outputStream.close()
                process.errorStream.close()
            }
        }
    }

    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = Coroutines.io {
        catchingDefault<BatteryLevel>(BatteryLevel.Unknown) {
            when (platform) {
                Platform.Windows -> Windows.readLevel()
                Platform.Linux -> LinuxBatteryReader.readLevel()
                Platform.MacOS -> MacOS.readLevel()
                else -> BatteryLevel.Unsupported
            }
        }
    }
}
