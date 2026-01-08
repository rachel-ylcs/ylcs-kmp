package love.yinlin.platform

import java.io.File

actual object NativeLibLoader {
    actual fun env(name: String) = System.loadLibrary(name)

    @Suppress("UnsafeDynamicallyLoadedCode")
    actual fun resource(name: String) {
        if (System.getProperty("native.library.resource.disabled") != null) {
            System.loadLibrary(name)
        }
        else {
            System.load("${System.getProperty("compose.application.resources.dir")}${File.separator}${System.mapLibraryName(name)}")
        }
    }
}