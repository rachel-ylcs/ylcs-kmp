@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

actual class PAGMarker(private val delegate: PlatformPAGMarker) {
    actual constructor(startTime: Long, duration: Long, comment: String) : this(PlatformPAGMarker(startTime, duration, comment))

    actual val startTime: Long get() = delegate.startTime
    actual val duration: Long get() = delegate.duration
    actual val comment: String get() = delegate.comment!!
}