package love.yinlin.compose.ui

expect class PAGVideoRange {
    constructor(startTime: Long = 0L, endTime: Long = 0L, playDuration: Long = 0L, reversed: Boolean = false)

    val startTime: Long
    val endTime: Long
    val playDuration: Long
    val reversed: Boolean
}