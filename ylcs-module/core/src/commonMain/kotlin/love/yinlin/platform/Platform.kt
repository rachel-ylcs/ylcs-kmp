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
    WebJs,
    WindowsNative,
    LinuxNative,
    MacOSNative;

    companion object {
        val Phone: Array<Platform> = [Android, IOS]
        val Desktop: Array<Platform> = [Windows, Linux, MacOS]
        val DesktopNative: Array<Platform> = [WindowsNative, LinuxNative, MacOSNative]
        val Web: Array<Platform> = [WebJs, WebWasm]

        fun contains(vararg filter: Platform): Boolean = platform in filter
        inline fun use(vararg filter: Platform, block: () -> Unit) = if (platform in filter) block() else Unit
        inline fun <T> use(vararg filter: Platform, ifTrue: () -> T, ifFalse: () -> T): T = if (platform in filter) ifTrue() else ifFalse()
        fun <T> use(vararg filter: Platform, ifTrue: T, ifFalse: T): T = if (platform in filter) ifTrue else ifFalse
        inline fun useNot(vararg filter: Platform, block: () -> Unit) = if (platform !in filter) block() else Unit
    }
}

expect val platform: Platform