package love.yinlin.platform

actual fun loadNativeLibrary(name: String) = System.loadLibrary(name)