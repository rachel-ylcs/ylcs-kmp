package love.yinlin.compose.ui

actual data class PAGMarker(private val delegate: PlatformPAGMarker) {
    actual constructor(startTime: Long, duration: Long, comment: String) : this(PlatformPAGMarker(startTime, duration, comment))

    actual val startTime: Long by delegate::startTime
    actual val duration: Long by delegate::duration
    actual val comment: String by delegate::comment
}