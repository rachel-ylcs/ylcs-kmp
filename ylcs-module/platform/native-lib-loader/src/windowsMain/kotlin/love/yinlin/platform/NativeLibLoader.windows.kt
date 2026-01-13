package love.yinlin.platform

import platform.windows.LoadLibraryW

actual object NativeLibLoader {
    actual fun env(name: String) {
        LoadLibraryW(name)
    }

    actual fun resource(name: String) {
        LoadLibraryW(name)
    }
}