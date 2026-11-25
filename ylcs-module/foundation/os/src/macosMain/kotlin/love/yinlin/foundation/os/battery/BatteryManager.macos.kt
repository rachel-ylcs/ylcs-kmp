package love.yinlin.foundation.os.battery

import kotlinx.cinterop.*
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.cleaning
import love.yinlin.foundation.PlatformContext
import platform.CoreFoundation.*
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSSelectorFromString
import platform.Foundation.lowPowerModeEnabled
import platform.IOKit.IOPSCopyPowerSourcesInfo
import platform.IOKit.IOPSCopyPowerSourcesList
import platform.IOKit.IOPSGetPowerSourceDescription

@OptIn(ExperimentalForeignApi::class)
actual object BatteryManager : MacBatteryReader<COpaquePointer>() {
    private inline fun <R> withString(value: String, block: (CFStringRef) -> R): R {
        val string = checkNotNull(CFStringCreateWithCString(null, value, kCFStringEncodingUTF8))
        return cleaning({ block(string) }) { CFRelease(string) }
    }

    private fun value(source: COpaquePointer, key: String): COpaquePointer? = withString(key) {
        CFDictionaryGetValue(source.reinterpret(), it)
    }

    override fun copyInfo(): COpaquePointer? = IOPSCopyPowerSourcesInfo()

    override fun copySources(info: COpaquePointer): COpaquePointer? = IOPSCopyPowerSourcesList(info)

    override fun count(sources: COpaquePointer): Long = CFArrayGetCount(sources.reinterpret())

    override fun description(info: COpaquePointer, sources: COpaquePointer, index: Long): COpaquePointer? {
        val source = CFArrayGetValueAtIndex(sources.reinterpret(), index) ?: return null
        return IOPSGetPowerSourceDescription(info, source)
    }

    override fun matches(source: COpaquePointer, key: String, expected: String): Boolean? {
        val value = value(source, key) ?: return null
        if (CFGetTypeID(value) != CFStringGetTypeID()) return false
        return withString(expected) { CFStringCompare(value.reinterpret(), it, 0uL) == kCFCompareEqualTo }
    }

    override fun boolean(source: COpaquePointer, key: String): Boolean? {
        val value = value(source, key) ?: return null
        return CFBooleanGetValue(value.reinterpret())
    }

    override fun number(source: COpaquePointer, key: String): Int? {
        val value = value(source, key) ?: return null
        return memScoped {
            val result = alloc<IntVar>()
            if (CFNumberGetValue(value.reinterpret(), kCFNumberIntType, result.ptr)) result.value
            else null
        }
    }

    override fun powerSaveMode(): Boolean? {
        val process = NSProcessInfo.processInfo
        return if (process.respondsToSelector(NSSelectorFromString("isLowPowerModeEnabled"))) process.lowPowerModeEnabled else null
    }

    override fun release(value: COpaquePointer) = CFRelease(value)

    actual suspend fun getBatteryInfo(context: PlatformContext): BatteryInfo = Coroutines.io { readInfo() }
}
