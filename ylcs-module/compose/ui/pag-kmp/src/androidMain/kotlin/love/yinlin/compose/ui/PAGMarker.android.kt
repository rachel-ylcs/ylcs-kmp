package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
actual class PAGMarker(private val delegate: PlatformPAGMarker) {
    actual constructor(startTime: Long, duration: Long, comment: String) : this(PlatformPAGMarker(startTime, duration, comment))

    actual val startTime: Long get() = delegate.mStartTime
    actual val duration: Long get() = delegate.mDuration
    actual val comment: String get() = delegate.mComment
}