package love.yinlin.foundation.os.battery

import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.cleaning
import love.yinlin.foundation.PlatformContext
import love.yinlin.platform.Platform
import love.yinlin.platform.ffi.*
import love.yinlin.platform.platform
import java.lang.foreign.ValueLayout

actual object BatteryManager {
    private object Windows {
        fun readInfo(): BatteryInfo = useMemory { arena ->
            val status = arena.allocate(12, 4)
            if (FFI.kernel32.GetSystemPowerStatus(status) as Int == Win32.FALSE) BatteryInfo()
            else WindowsBatteryReader.readInfo(
                status.get(ValueLayout.JAVA_BYTE, 0).toUByte().toInt(),
                status.get(ValueLayout.JAVA_BYTE, 1).toUByte().toInt(),
                status.get(ValueLayout.JAVA_BYTE, 2).toUByte().toInt(),
                status.get(ValueLayout.JAVA_BYTE, 3).toUByte().toInt(),
                status.get(ValueLayout.JAVA_INT, 4).toUInt(),
            )
        }
    }

    private object MacOS : MacBatteryReader<Address>() {
        private fun <R> withString(value: String, block: (Address) -> R): R = useMemory { arena ->
            val string = FFI.coreFoundation.CFStringCreateWithCString(Address.NULL, arena.astr(value), 0x08000100) as Address
            check(string != Address.NULL)
            cleaning({ block(string) }) { FFI.coreFoundation.CFRelease(string) }
        }

        private fun value(source: Address, key: String): Address? = withString(key) {
            (FFI.coreFoundation.CFDictionaryGetValue(source, it) as Address).takeUnless { address -> address == Address.NULL }
        }

        override fun copyInfo(): Address? = (FFI.iokit.IOPSCopyPowerSourcesInfo() as Address).takeUnless { it == Address.NULL }

        override fun copySources(info: Address): Address? = (FFI.iokit.IOPSCopyPowerSourcesList(info) as Address).takeUnless { it == Address.NULL }

        override fun count(sources: Address): Long = FFI.coreFoundation.CFArrayGetCount(sources) as Long

        override fun description(info: Address, sources: Address, index: Long): Address? {
            val source = FFI.coreFoundation.CFArrayGetValueAtIndex(sources, index) as Address
            if (source == Address.NULL) return null
            return (FFI.iokit.IOPSGetPowerSourceDescription(info, source) as Address).takeUnless { it == Address.NULL }
        }

        override fun matches(source: Address, key: String, expected: String): Boolean? {
            val value = value(source, key) ?: return null
            return withString(expected) { (FFI.coreFoundation.CFEqual(value, it) as Byte) != 0.toByte() }
        }

        override fun boolean(source: Address, key: String): Boolean? {
            val value = value(source, key) ?: return null
            return (FFI.coreFoundation.CFBooleanGetValue(value) as Byte) != 0.toByte()
        }

        override fun number(source: Address, key: String): Int? {
            val value = value(source, key) ?: return null

            return useMemory { arena ->
                val result = arena.allocate(ValueLayout.JAVA_INT)
                if ((FFI.coreFoundation.CFNumberGetValue(value, 9L, result) as Byte) != 0.toByte()) result.get(ValueLayout.JAVA_INT, 0) else null
            }
        }

        override fun powerSaveMode(): Boolean? = useMemory { arena ->
            val cls = withString("NSProcessInfo") { FFI.foundation.NSClassFromString(it) as Address }
            if (cls == Address.NULL) return@useMemory null

            val objc = FFI.objc
            val processInfo = objc.sel_registerName(arena.astr("processInfo")) as Address
            val process = objc.objcPointerMessage(cls, processInfo) as Address
            if (process == Address.NULL) return@useMemory null

            val lowPower = objc.sel_registerName(arena.astr("isLowPowerModeEnabled")) as Address
            val responds = objc.sel_registerName(arena.astr("respondsToSelector:")) as Address
            if ((objc.objcRespondsToSelector(process, responds, lowPower) as Byte) == 0.toByte()) return@useMemory null

            (objc.objcBooleanMessage(process, lowPower) as Byte) != 0.toByte()
        }

        override fun release(value: Address) {
            FFI.coreFoundation.CFRelease(value)
        }
    }

    actual suspend fun getBatteryInfo(context: PlatformContext): BatteryInfo = Coroutines.io {
        catchingDefault(BatteryInfo()) {
            when (platform) {
                Platform.Windows -> Windows.readInfo()
                Platform.Linux -> LinuxBatteryReader.readInfo()
                Platform.MacOS -> MacOS.readInfo()
                else -> BatteryInfo(level = BatteryLevel.Unsupported)
            }
        }
    }
}
