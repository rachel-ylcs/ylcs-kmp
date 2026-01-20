package love.yinlin.compose.ui

actual data class PAGMarker(private val delegate: PlatformPAGMarker) {
    actual constructor(startTime: Long, duration: Long, comment: String) : this(PlatformPAGMarker(startTime, duration, comment))

    actual val startTime: Long by delegate::mStartTime
    actual val duration: Long by delegate::mDuration
    actual val comment: String by delegate::mComment
}