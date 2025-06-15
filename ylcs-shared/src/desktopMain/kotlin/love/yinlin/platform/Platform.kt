package love.yinlin.platform

actual val platform: Platform = System.getProperty("os.name").let {
    when {
        it.startsWith("Windows") -> Platform.Windows
        it.startsWith("Mac") -> Platform.MacOS
        else -> Platform.Linux
    }
}