package love.yinlin.foundation.os.battery

import kotlinx.cinterop.*
import love.yinlin.extension.cleaning
import love.yinlin.foundation.os.iokit.IOPSCopyPowerSourcesInfo
import love.yinlin.foundation.os.iokit.IOPSCopyPowerSourcesList
import love.yinlin.foundation.os.iokit.IOPSGetPowerSourceDescription
import platform.CoreFoundation.*

@OptIn(ExperimentalForeignApi::class)
internal object MacBatteryApi : MacBatteryReader<COpaquePointer>() {
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

    override fun isInternal(source: COpaquePointer): Boolean? {
        val transport = value(source, "Transport Type") ?: return null
        return withString("Internal") { CFEqual(transport, it).toInt() != 0 }
    }

    override fun isPresent(source: COpaquePointer): Boolean {
        val present = value(source, "Is Present") ?: return true
        return CFBooleanGetValue(present.reinterpret()).toInt() != 0
    }

    override fun number(source: COpaquePointer, key: String): Int? {
        val value = value(source, key) ?: return null
        return memScoped {
            val result = alloc<IntVar>()
            if (CFNumberGetValue(value.reinterpret(), kCFNumberIntType, result.ptr).toInt() != 0) result.value
            else null
        }
    }

    override fun release(value: COpaquePointer) = CFRelease(value)
}
