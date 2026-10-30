package love.yinlin.foundation

import platform.windows.LoadLibraryW

actual object LibLoader {
    actual fun env(name: String) {
        LoadLibraryW(name)
    }

    actual fun resource(name: String) {
        LoadLibraryW(name)
    }
}