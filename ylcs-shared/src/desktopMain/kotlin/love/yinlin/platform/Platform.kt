package love.yinlin.platform

actual val platform: Platform = System.getProperty("os.name").lowercase().let {
    when {
        it.startsWith("windows") -> Windows
        it.startsWith("mac") -> MacOS
        else -> Linux
    }
}