package androidx.compose.animation.graphics.vector

import androidx.compose.animation.core.RepeatMode
import kotlin.math.ceil

internal val Timestamp<*>.endsAtStart: Boolean
    get() = repeatMode == RepeatMode.Reverse && repeatCount > 0 && repeatCount % 2 != 0

/** Exact repeat boundaries belong to the preceding iteration, including the final frame. */
internal fun pathAnimatorFraction(timeMillis: Float, timestamp: Timestamp<*>): Float {
    require(timestamp.durationMillis >= 0 && timestamp.repeatCount >= RepeatCountInfinite) { "Invalid path animator duration or repeat count" }
    if (timeMillis < timestamp.timeMillis) return 0f
    val iterations = if (timestamp.repeatCount == RepeatCountInfinite) Long.MAX_VALUE else timestamp.repeatCount.toLong() + 1L
    if (timestamp.durationMillis == 0) return if (timestamp.repeatMode == RepeatMode.Reverse && iterations % 2L == 0L) 0f else 1f
    require(timestamp.durationMillis > 0 && iterations > 0) { "Invalid path animator duration or repeat count" }
    val progress = ((timeMillis.toDouble() - timestamp.timeMillis) / timestamp.durationMillis).coerceIn(0.0, iterations.toDouble())
    val iteration = (ceil(progress).toLong() - 1L).coerceAtLeast(0L)
    val fraction = (progress - iteration).toFloat().coerceIn(0f, 1f)
    return if (timestamp.repeatMode == RepeatMode.Reverse && iteration % 2L != 0L) 1f - fraction else fraction
}

internal fun saturatedAnimationTime(first: Int, second: Int): Int = (first.toLong() + second).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

internal fun animatorTotalDuration(duration: Int, startDelay: Int, repeatCount: Int): Int {
    require(duration >= 0 && startDelay >= 0 && repeatCount >= RepeatCountInfinite) { "Invalid animator duration, delay or repeat count" }
    if (repeatCount == RepeatCountInfinite) return Int.MAX_VALUE
    return (startDelay.toLong() + duration.toLong() * (repeatCount.toLong() + 1L)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
