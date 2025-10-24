package love.yinlin.extension

val Long.fileSizeString: String get() = if (this < 1024) "${this}B"
else if (this < 1024 * 1024) "${this / 1024}KB"
else if (this < 1024 * 1024 * 1024) "${this / (1024 * 1024)}MB"
else "${this / (1024 * 1024 * 1024)}GB"

val Long.timeString: String get() {
    val hours = (this / (1000 * 60 * 60)).toInt()
    val minutes = (this % (1000 * 60 * 60) / (1000 * 60)).toInt()
    val seconds = (this % (1000 * 60) / 1000).toInt()
    return buildString(capacity = 8) {
        if (hours > 0) {
            if (hours < 10) {
                append('0')
                append('0' + hours)
            }
            else {
                append('0' + hours / 10)
                append('0' + hours % 10)
            }
            append(':')
        }
        if (minutes < 10) {
            append('0')
            append('0' + minutes)
        }
        else {
            append('0' + minutes / 10)
            append('0' + minutes % 10)
        }
        append(':')
        if (seconds < 10) {
            append('0')
            append('0' + seconds)
        }
        else {
            append('0' + seconds / 10)
            append('0' + seconds % 10)
        }
    }
}

fun String.filenameOrRandom(ext: String): String = this.substringAfterLast('/').substringBefore('?').ifEmpty { "${DateEx.CurrentLong}$ext" }