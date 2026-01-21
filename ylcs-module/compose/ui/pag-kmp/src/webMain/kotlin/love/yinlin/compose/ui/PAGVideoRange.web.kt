package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

@OptIn(ExperimentalWasmJsInterop::class)
private fun makePAGVideoRange(startTime: Double, endTime: Double, playDuration: Double, reversed: Boolean): PlatformPAGVideoRange =
    js("({ startTime: startTime, endTime: endTime, playDuration: playDuration, reversed: reversed })")

@Stable
actual class PAGVideoRange(private val delegate: PlatformPAGVideoRange) {
    actual constructor(startTime: Long, endTime: Long, playDuration: Long, reversed: Boolean) : this(makePAGVideoRange(startTime.toDouble(), endTime.toDouble(), playDuration.toDouble(), reversed))

    actual val startTime: Long get() = delegate.startTime.toLong()
    actual val endTime: Long get() = delegate.endTime.toLong()
    actual val playDuration: Long get() = delegate.playDuration.toLong()
    actual val reversed: Boolean get() = delegate.reversed
}