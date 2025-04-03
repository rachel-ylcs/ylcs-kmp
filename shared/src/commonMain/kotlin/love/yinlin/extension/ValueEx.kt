package love.yinlin.extension

val Long.fileSizeString: String get() = if (this < 1024) "${this}B"
    else if (this < 1024 * 1024) "${this / 1024}KB"
    else if (this < 1024 * 1024 * 1024) "${this / (1024 * 1024)}MB"
    else "${this / (1024 * 1024 * 1024)}GB"