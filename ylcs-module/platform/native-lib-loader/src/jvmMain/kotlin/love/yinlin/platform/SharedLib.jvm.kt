package love.yinlin.platform

import java.io.File

actual sealed interface SharedLib {
    actual fun load()

    actual class Env actual constructor(private val name: String) : SharedLib {
        actual override fun load() = System.loadLibrary(name)
    }

    actual class Resource actual constructor(private val name: String) : SharedLib {
        actual override fun load() = System.load("${System.getProperty("compose.application.resources.dir")}${File.separator}${System.mapLibraryName(name)}")
    }
}