package love.yinlin.foundation.os.battery

import love.yinlin.extension.cleaning
import love.yinlin.extension.cleaning
import love.yinlin.platform.ffi.*
import java.lang.foreign.ValueLayout

internal object MacBatteryApi : MacBatteryReader<Address>() {
    private object IOKit : IOKitLibrary()

    private object CoreFoundation : CoreFoundationLibrary() {
        val CFArrayGetCount by func(NativeType.Pointer, retType = NativeType.Long)
        val CFArrayGetValueAtIndex by func(NativeType.Pointer, NativeType.Long, retType = NativeType.Pointer)
        val CFStringCreateWithCString by func(NativeType.Pointer, NativeType.Pointer, NativeType.Int, retType = NativeType.Pointer)
        val CFDictionaryGetValue by func(NativeType.Pointer, NativeType.Pointer, retType = NativeType.Pointer)
        val CFEqual by func(NativeType.Pointer, NativeType.Pointer, retType = NativeType.Byte)
        val CFBooleanGetValue by func(NativeType.Pointer, retType = NativeType.Byte)
        val CFNumberGetValue by func(NativeType.Pointer, NativeType.Long, NativeType.Pointer, retType = NativeType.Byte)
        val CFRelease by func(NativeType.Pointer)

        fun <R> withString(value: String, block: (Address) -> R): R = useMemory { arena ->
            val string = CFStringCreateWithCString(Address.NULL, arena.astr(value), 0x08000100) as Address
            check(string != Address.NULL)
            cleaning({ block(string) }) { CFRelease(string) }
        }

        fun value(source: Address, key: String): Address? = withString(key) {
            (CFDictionaryGetValue(source, it) as Address).takeUnless { address -> address == Address.NULL }
        }
    }

    override fun copyInfo(): Address? =
        (IOKit.IOPSCopyPowerSourcesInfo() as Address).takeUnless { it == Address.NULL }

    override fun copySources(info: Address): Address? =
        (IOKit.IOPSCopyPowerSourcesList(info) as Address).takeUnless { it == Address.NULL }

    override fun count(sources: Address): Long = CoreFoundation.CFArrayGetCount(sources) as Long

    override fun description(info: Address, sources: Address, index: Long): Address? {
        val source = CoreFoundation.CFArrayGetValueAtIndex(sources, index) as Address
        if (source == Address.NULL) return null
        return (IOKit.IOPSGetPowerSourceDescription(info, source) as Address).takeUnless { it == Address.NULL }
    }

    override fun isInternal(source: Address): Boolean? {
        val transport = CoreFoundation.value(source, "Transport Type") ?: return null
        return CoreFoundation.withString("Internal") { (CoreFoundation.CFEqual(transport, it) as Byte) != 0.toByte() }
    }

    override fun isPresent(source: Address): Boolean {
        val present = CoreFoundation.value(source, "Is Present") ?: return true
        return (CoreFoundation.CFBooleanGetValue(present) as Byte) != 0.toByte()
    }

    override fun number(source: Address, key: String): Int? {
        val value = CoreFoundation.value(source, key) ?: return null
        return CoreFoundation.useMemory { arena ->
            val result = arena.allocate(ValueLayout.JAVA_INT)
            // kCFNumberIntType = 9，CFNumberType 的底层类型为 CFIndex。
            if ((CoreFoundation.CFNumberGetValue(value, 9L, result) as Byte) != 0.toByte()) result.get(ValueLayout.JAVA_INT, 0)
            else null
        }
    }

    override fun release(value: Address) {
        CoreFoundation.CFRelease(value)
    }
}
