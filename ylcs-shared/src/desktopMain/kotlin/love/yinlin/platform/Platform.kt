package love.yinlin.platform

actual val platform: Platform = System.getProperty("os.name").lowercase().let {
    when {
        it.startsWith("windows") -> Platform.Windows
        it.startsWith("mac") -> Platform.MacOS
        else -> Platform.Linux
    }
}