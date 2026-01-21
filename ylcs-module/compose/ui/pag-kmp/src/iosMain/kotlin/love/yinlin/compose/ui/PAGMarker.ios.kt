@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
actual class PAGMarker(private val delegate: PlatformPAGMarker) {
    actual constructor(startTime: Long, duration: Long, comment: String) : this(makePlatformPAGMarker(startTime, duration, comment))

    actual val startTime: Long get() = delegate.startTime
    actual val duration: Long get() = delegate.duration
    actual val comment: String get() = delegate.comment!!
}