package love.yinlin.compose.ui

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

@OptIn(ExperimentalWasmJsInterop::class)
private fun makePAGMarker(startTime: Double, duration: Double, comment: String): PlatformPAGMarker =
    js("({ startTime: startTime, duration: duration, comment: comment })")

actual data class PAGMarker(private val delegate: PlatformPAGMarker) {
    actual constructor(startTime: Long, duration: Long, comment: String) : this(makePAGMarker(startTime.toDouble(), duration.toDouble(), comment))

    actual val startTime: Long get() = delegate.startTime.toLong()
    actual val duration: Long get() = delegate.duration.toLong()
    actual val comment: String by delegate::comment
}