package love.yinlin.compose.ui

actual data class PAGVideoRange(private val delegate: PlatformPAGVideoRange) {
    actual constructor(startTime: Long, endTime: Long, playDuration: Long, reversed: Boolean) : this(PlatformPAGVideoRange(startTime, endTime, playDuration, reversed))

    actual val startTime: Long by delegate::startTime
    actual val endTime: Long by delegate::endTime
    actual val playDuration: Long by delegate::playDuration
    actual val reversed: Boolean by delegate::reversed
}