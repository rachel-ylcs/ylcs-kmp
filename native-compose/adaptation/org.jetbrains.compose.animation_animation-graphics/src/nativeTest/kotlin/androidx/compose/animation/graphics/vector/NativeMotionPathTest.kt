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
        val holder = PropertyValuesHolder2D("translateX", "translateY", [PathNode.MoveTo(0f, 0f), PathNode.LineTo(100f, 0f), PathNode.LineTo(100f, 100f)]) { it * it }
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
    fun reverseRepeatPreservesItsTerminalValueAndExactBoundaries() {
        val holder = PropertyValuesHolderFloat("x", [Keyframe(0f, 0f, LinearEasing), Keyframe(1f, 1f, LinearEasing)])
        val timestamp = Timestamp(10, 100, 1, RepeatMode.Reverse, holder)
        assertEquals(0f, pathAnimatorFraction(9f, timestamp))
        assertEquals(1f, pathAnimatorFraction(110f, timestamp))
        assertEquals(0.5f, pathAnimatorFraction(160f, timestamp))
        assertEquals(0f, pathAnimatorFraction(210f, timestamp))
        assertEquals(0f, timestamp.floatEndpoint(true))
    }
}
