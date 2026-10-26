package androidx.compose.animation.graphics.vector

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedDurationBasedAnimationSpec

internal class MotionPathValuesHolder(
    val propertyName: String,
    private val path: MotionPath,
    private val xAxis: Boolean,
    private val easing: Easing,
) : PropertyValuesHolder<Float>() {
    fun endpoint(atEnd: Boolean): Float = path.coordinate(if (atEnd) 1f else 0f, xAxis)

    fun asAnimationSpec(duration: Int): DurationBasedAnimationSpec<Float> = object : DurationBasedAnimationSpec<Float> {
        init { require(duration >= 0) { "Motion path duration must not be negative" } }

        override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<Float, V>): VectorizedDurationBasedAnimationSpec<V> = object : VectorizedDurationBasedAnimationSpec<V> {
            override val durationMillis = duration
            override val delayMillis = 0
            private val durationNanos = duration.toLong() * 1_000_000L

            private fun value(time: Long): Float {
                val fraction = if (durationNanos == 0L) { if (time < 0L) 0f else 1f } else (time.coerceIn(0L, durationNanos).toDouble() / durationNanos).toFloat()
                return path.coordinate(easing.transform(fraction).coerceIn(0f, 1f), xAxis)
            }

            override fun getValueFromNanos(playTimeNanos: Long, initialValue: V, targetValue: V, initialVelocity: V): V = converter.convertToVector(value(playTimeNanos))

            override fun getVelocityFromNanos(playTimeNanos: Long, initialValue: V, targetValue: V, initialVelocity: V): V {
                if (durationNanos == 0L || playTimeNanos !in 0L .. durationNanos) return converter.convertToVector(0f)
                val start = (playTimeNanos - 1_000_000L).coerceAtLeast(0L)
                val end = (playTimeNanos + 1_000_000L).coerceAtMost(durationNanos)
                return converter.convertToVector(((value(end) - value(start)) * 1_000_000_000.0 / (end - start)).toFloat())
            }
        }
    }
}

internal fun PropertyValuesHolder2D.splitMotionPath(): List<MotionPathValuesHolder> {
    val path = MotionPath(pathData)
    return buildList {
        if (xPropertyName.isNotEmpty()) add(MotionPathValuesHolder(xPropertyName, path, true, interpolator))
        if (yPropertyName.isNotEmpty()) add(MotionPathValuesHolder(yPropertyName, path, false, interpolator))
    }
}

internal fun PropertyValuesHolder<Float>.floatEndpoint(atEnd: Boolean): Float = when (this) {
    is MotionPathValuesHolder -> endpoint(atEnd)
    is PropertyValuesHolderFloat -> if (atEnd) animatorKeyframes.last().value else animatorKeyframes.first().value
    else -> error("Unexpected float property holder: $this")
}

internal fun Timestamp<Float>.floatEndpoint(atEnd: Boolean): Float = holder.floatEndpoint(atEnd && !endsAtStart)
