@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

actual class PAGVideoRange(private val delegate: PlatformPAGVideoRange) {
    actual constructor(startTime: Long, endTime: Long, playDuration: Long, reversed: Boolean) : this(makePlatformPAGVideoRange(startTime, endTime, playDuration, reversed))

    actual val startTime: Long get() = delegate.startTime
    actual val endTime: Long get() = delegate.endTime
    actual val playDuration: Long get() = delegate.playDuration
    actual val reversed: Boolean get() = delegate.reversed.toBoolean()
}