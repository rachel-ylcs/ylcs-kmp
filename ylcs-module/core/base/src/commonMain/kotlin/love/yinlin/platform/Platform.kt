package love.yinlin.platform

import kotlinx.serialization.Serializable

@Serializable
enum class Platform {
    Android,
    IOS,
    Windows,
    Linux,
    MacOS,
    WebWasm,
    WindowsNative,
    LinuxNative,
    MacOSNative;

    companion object {
        val Phone = arrayOf(Android, IOS)
        val Desktop = arrayOf(Windows, Linux, MacOS)
        val DesktopNative = arrayOf(WindowsNative, LinuxNative, MacOSNative)

        fun fromInt(value: Int): Platform? = when (value) {
            Android.ordinal -> Android
            IOS.ordinal -> IOS
            Windows.ordinal -> Windows
            Linux.ordinal -> Linux
            MacOS.ordinal -> MacOS
            WebWasm.ordinal -> WebWasm
            WindowsNative.ordinal -> WindowsNative
            LinuxNative.ordinal -> LinuxNative
            MacOSNative.ordinal -> MacOSNative
            else -> null
        }

        fun contains(vararg filter: Platform): Boolean = platform in filter
        inline fun use(vararg filter: Platform, block: () -> Unit) = if (platform in filter) block() else Unit
        inline fun <T> use(vararg filter: Platform, ifTrue: () -> T, ifFalse: () -> T): T = if (platform in filter) ifTrue() else ifFalse()
        fun <T> use(vararg filter: Platform, ifTrue: T, ifFalse: T): T = if (platform in filter) ifTrue else ifFalse
        inline fun useNot(vararg filter: Platform, block: () -> Unit) = if (platform !in filter) block() else Unit
    }
}

expect val platform: Platform