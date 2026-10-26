@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package androidx.compose.animation.graphics.vector

import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import kotlin.test.*

class NativeMotionPathTest {
    @BeforeTest
    fun installGraphicsBackend() { registerSkikoComposeImplementation() }

    @Test
    fun appliesEasingToTheWholePathAndKeepsAxesAligned() {
        val holder = PropertyValuesHolder2D("translateX", "translateY", [PathNode.MoveTo(0f, 0f), PathNode.LineTo(100f, 0f), PathNode.LineTo(100f, 100f)], Easing { it * it })
        val axes = holder.splitMotionPath()
        val x = axes[0].asAnimationSpec(1000).vectorize(Float.VectorConverter)
        val y = axes[1].asAnimationSpec(1000).vectorize(Float.VectorConverter)
        val zero = AnimationVector1D(0f)
        assertEquals(50f, x.getValueFromNanos(500_000_000L, zero, zero, zero).value, 0.01f)
        assertEquals(0f, y.getValueFromNanos(500_000_000L, zero, zero, zero).value, 0.01f)
    }

    @Test
    fun contoursDoNotCreateAnArtificialConnectingLine() {
        val path = MotionPath([PathNode.MoveTo(0f, 0f), PathNode.LineTo(10f, 0f), PathNode.MoveTo(100f, 100f), PathNode.LineTo(110f, 100f)])
        assertEquals(100f, path.coordinate(0.5f, true), 0.01f)
        assertEquals(100f, path.coordinate(0.5f, false), 0.01f)
        assertEquals(105f, path.coordinate(0.75f, true), 0.01f)
    }

    @Test
    fun zeroDurationHoldsTheStartUntilItsScheduledTime() {
        val holder = MotionPathValuesHolder("x", MotionPath([PathNode.MoveTo(5f, 0f), PathNode.LineTo(20f, 0f)]), true, LinearEasing)
        val spec = holder.asAnimationSpec(0).vectorize(Float.VectorConverter)
        val zero = AnimationVector1D(0f)
        assertEquals(5f, spec.getValueFromNanos(-1L, zero, zero, zero).value)
        assertEquals(20f, spec.getValueFromNanos(0L, zero, zero, zero).value)
        assertEquals(0f, spec.getVelocityFromNanos(0L, zero, zero, zero).value)
    }

    @Test
    fun reverseRepeatPreservesItsTerminalValueAndExactBoundaries() {
        val holder = PropertyValuesHolderFloat("x", [Keyframe(0f, 0f, LinearEasing), Keyframe(1f, 1f, LinearEasing)])
        val timestamp = Timestamp(10, 100, 1, RepeatMode.Reverse, holder)
        assertEquals(0f, pathAnimatorFraction(9f, timestamp))
        assertEquals(1f, pathAnimatorFraction(110f, timestamp))
        assertEquals(0.5f, pathAnimatorFraction(160f, timestamp))
        assertEquals(0f, pathAnimatorFraction(210f, timestamp))
        assertEquals(0f, timestamp.floatEndpoint(true))
    }

    @Test
    fun saturatesLargeDurationsInsteadOfWrappingNegative() {
        assertEquals(210, animatorTotalDuration(100, 10, 1))
        assertEquals(Int.MAX_VALUE, animatorTotalDuration(Int.MAX_VALUE, 1, 2))
        assertEquals(Int.MAX_VALUE, animatorTotalDuration(100, 0, Int.MAX_VALUE))
        assertEquals(Int.MAX_VALUE, saturatedAnimationTime(Int.MAX_VALUE, 1))
        val animator = ObjectAnimator(100, 0, Int.MAX_VALUE, RepeatMode.Restart, [])
        assertEquals(Int.MAX_VALUE, AnimatorSet([animator, animator], Ordering.Sequentially).totalDuration)
    }

    @Test
    fun singleMorphKeyframeAndEmptyPathsStayDefined() {
        val value: List<PathNode> = [PathNode.MoveTo(3f, 4f)]
        val holder = PropertyValuesHolderPath("pathData", [Keyframe(0.5f, value, LinearEasing)])
        assertEquals(value, holder.interpolate(0f))
        assertEquals(value, holder.interpolate(1f))
        assertEquals(0f, MotionPath([]).coordinate(0.5f, true))
        assertEquals(3f, MotionPath(value).coordinate(0.5f, true))
    }

    @Test
    fun rejectsInvalidTimingBeforeAnyAnimationRuns() {
        assertFailsWith<IllegalArgumentException> { animatorTotalDuration(1, -1, 0) }
        assertFailsWith<IllegalArgumentException> { animatorTotalDuration(1, 0, -2) }
        assertFailsWith<IllegalArgumentException> { MotionPath(List(65537) { PathNode.MoveTo(0f, 0f) }) }
    }
}
