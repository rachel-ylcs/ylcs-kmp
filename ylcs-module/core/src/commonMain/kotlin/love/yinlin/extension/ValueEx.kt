package love.yinlin.extension

// Bit Cast

fun merge64(v1: Int, v2: Int): Long = (v1.toLong() shl 32) or (v2.toLong() and 0xFFFFFFFF)
fun merge64(v1: UInt, v2: UInt): Long = (v1.toLong() shl 32) or (v2.toLong() and 0xFFFFFFFF)
fun merge64(v1: Float, v2: Float): Long = (v1.toRawBits().toLong() shl 32) or (v2.toRawBits().toLong() and 0xFFFFFFFF)
val Long.int1: Int get() = (this shr 32).toInt()
val Long.int2: Int get() = (this and 0xFFFFFFFF).toInt()
val Long.uint1: UInt get() = this.int1.toUInt()
val Long.uint2: UInt get() = this.int2.toUInt()
val Long.float1: Float get() =  Float.fromBits((this shr 32).toInt())
val Long.float2: Float get() =  Float.fromBits((this and 0xFFFFFFFF).toInt())

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