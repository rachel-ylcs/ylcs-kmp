package love.yinlin.compose.ui.node

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import kotlin.jvm.JvmName

inline fun Modifier.fastRotate(crossinline degreeProvider: () -> Float?) = this.graphicsLayer {
    degreeProvider()?.let { rotationZ = it }
}

inline fun Modifier.fastRotate(crossinline xProvider: () -> Float?, crossinline yProvider: () -> Float?, crossinline zProvider: () -> Float?) = this.graphicsLayer {
    xProvider()?.let { rotationX = it }
    yProvider()?.let { rotationY = it }
    zProvider()?.let { rotationZ = it }
}

fun Modifier.fastRotate(animatable: Animatable<Float, AnimationVector1D>) = this.graphicsLayer {
    rotationZ = animatable.value
}

fun Modifier.fastRotate(state: State<Float>) = this.graphicsLayer {
    rotationZ = state.value
}

inline fun Modifier.fastScale(crossinline scaleXProvider: () -> Float?, crossinline scaleYProvider: () -> Float?) = this.graphicsLayer {
    scaleXProvider()?.let { scaleX = it }
    scaleYProvider()?.let { scaleY = it }
}

inline fun Modifier.fastScale(crossinline scaleProvider: () -> Float?) = this.graphicsLayer {
    scaleProvider()?.let {
        scaleX = it
        scaleY = it
    }
}

fun Modifier.fastScale(animatable: Animatable<Float, AnimationVector1D>) = this.graphicsLayer {
    val value = animatable.value
    scaleX = value
    scaleY = value
}

fun Modifier.fastScale(state: State<Float>) = this.graphicsLayer {
    val value = state.value
    scaleX = value
    scaleY = value
}

inline fun Modifier.fastAlpha(crossinline alphaProvider: () -> Float?) = this.graphicsLayer {
    alphaProvider()?.let { alpha = it }
}

fun Modifier.fastAlpha(animatable: Animatable<Float, AnimationVector1D>) = this.graphicsLayer {
    alpha = animatable.value
}

fun Modifier.fastAlpha(state: State<Float>) = this.graphicsLayer {
    alpha = state.value
}

inline fun Modifier.fastBackground(crossinline colorProvider: () -> Color?) = this.drawBehind {
    colorProvider()?.let { drawRect(it) }
}

fun Modifier.fastBackground(state: State<Color>) = this.drawBehind {
    drawRect(state.value)
}

inline fun Modifier.fastClip(crossinline shapeProvider: () -> Shape?) = this.graphicsLayer {
    shapeProvider()?.let {
        shape = it
        clip = true
    }
}

fun Modifier.fastClipCircle() = this.graphicsLayer {
    shape = CircleShape
    clip = true
}

inline fun Modifier.fastOffsetX(crossinline offsetXProvider: () -> Float?) = this.graphicsLayer {
    offsetXProvider()?.let { translationX = it }
}

inline fun Modifier.fastOffsetXDp(crossinline offsetXProvider: () -> Dp?) = this.graphicsLayer {
    offsetXProvider()?.let { translationX = it.toPx() }
}

@JvmName("fastOffsetXByStatePx")
fun Modifier.fastOffsetX(state: State<Float>) = this.graphicsLayer {
    translationX = state.value
}

@JvmName("fastOffsetXByStatePxInt")
fun Modifier.fastOffsetX(state: State<Int>) = this.graphicsLayer {
    translationX = state.value.toFloat()
}

@JvmName("fastOffsetXByStateDp")
fun Modifier.fastOffsetX(state: State<Dp>) = this.graphicsLayer {
    translationX = state.value.toPx()
}

inline fun Modifier.fastOffsetY(crossinline offsetYProvider: () -> Float?) = this.graphicsLayer {
    offsetYProvider()?.let { translationY = it }
}

inline fun Modifier.fastOffsetYDp(crossinline offsetYProvider: () -> Dp?) = this.graphicsLayer {
    offsetYProvider()?.let { translationY = it.toPx() }
}

@JvmName("fastOffsetYByStatePx")
fun Modifier.fastOffsetY(state: State<Float>) = this.graphicsLayer {
    translationY = state.value
}

@JvmName("fastOffsetYByStatePxInt")
fun Modifier.fastOffsetY(state: State<Int>) = this.graphicsLayer {
    translationY = state.value.toFloat()
}

@JvmName("fastOffsetYByStateDp")
fun Modifier.fastOffsetYDp(state: State<Dp>) = this.graphicsLayer {
    translationY = state.value.toPx()
}

inline fun Modifier.fastOffset(crossinline offsetXProvider: () -> Float?, crossinline offsetYProvider: () -> Float?) = this.graphicsLayer {
    offsetXProvider()?.let { translationX = it }
    offsetYProvider()?.let { translationY = it }
}