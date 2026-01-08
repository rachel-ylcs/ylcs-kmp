package love.yinlin.platform

sealed interface NativeWindowImpl {
    fun updateClickThrough(handle: Long, enabled: Boolean)
}

object NativeWindow : NativeWindowImpl by when (platform) {
    Platform.Windows -> WindowsNativeWindow
    Platform.Linux -> LinuxNativeWindow
    Platform.MacOS -> MacOSNativeWindow
    else -> error(UnsupportedPlatformText)
}