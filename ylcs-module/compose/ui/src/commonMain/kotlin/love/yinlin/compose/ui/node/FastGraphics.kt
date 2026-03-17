package love.yinlin.compose.ui.node

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.Theme
import love.yinlin.compose.platform.inspector
import kotlin.jvm.JvmName

val NullFloatProvider: GraphicsLayerScope.() -> Float? = { null }

// FastRotate

private class FastRotateNode(var angleProvider: () -> Float?) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        withTransform({
            angleProvider()?.let { rotate(degrees = it) }
        }) {
            this@draw.drawContent()
        }
    }
}

private data class FastRotateElement(val angleProvider: () -> Float?) : ModifierNodeElement<FastRotateNode>() {
    override fun create(): FastRotateNode = FastRotateNode(angleProvider)
    override fun update(node: FastRotateNode) {
        node.angleProvider = angleProvider
    }
    override fun InspectorInfo.inspectableProperties() = inspector("fastRotate") {
        "angleProvider" bind angleProvider
    }
}

@Stable
fun Modifier.fastRotate(degreeProvider: () -> Float?) =
    this then FastRotateElement(degreeProvider)

@Stable
fun Modifier.fastRotate(animatable: Animatable<Float, AnimationVector1D>) =
    this then FastRotateElement { animatable.value }

@Stable
fun Modifier.fastRotate(state: State<Float>) =
    this then FastRotateElement { state.value }

@Stable
fun Modifier.fastAnimateRotate(degree: Float) = composed {
    val state = animateFloatAsState(
        targetValue = degree,
        animationSpec = tween(Theme.animation.duration.default)
    )
    this then FastRotateElement { state.value }
}

// FastScale

private class FastScaleNode(var scaleXProvider: () -> Float?, var scaleYProvider: () -> Float?) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        withTransform({
            val scaleX = scaleXProvider() ?: 1f
            val scaleY = scaleYProvider() ?: 1f
            if (scaleX != 1f || scaleY != 1f) scale(scaleX, scaleY)
        }) {
            this@draw.drawContent()
        }
    }
}

private data class FastScaleElement(val scaleXProvider: () -> Float?, val scaleYProvider: () -> Float? = scaleXProvider) : ModifierNodeElement<FastScaleNode>() {
    override fun create(): FastScaleNode = FastScaleNode(scaleXProvider, scaleYProvider)
    override fun update(node: FastScaleNode) {
        node.scaleXProvider = scaleXProvider
        node.scaleYProvider = scaleYProvider
    }
    override fun InspectorInfo.inspectableProperties() = inspector("fastScale") {
        "scaleXProvider" bind scaleXProvider
        "scaleYProvider" bind scaleYProvider
    }
}

@Stable
fun Modifier.fastScale(scaleXProvider: () -> Float?, scaleYProvider: () -> Float?) =
    this then FastScaleElement(scaleXProvider, scaleYProvider)

@Stable
fun Modifier.fastScale(scaleProvider: () -> Float?) =
    this then FastScaleElement(scaleProvider)

@Stable
fun Modifier.fastScale(animatable: Animatable<Float, AnimationVector1D>) =
    this then FastScaleElement({ animatable.value })

@Stable
fun Modifier.fastScale(state: State<Float>) =
    this then FastScaleElement({ state.value })

@Stable
fun Modifier.fastAnimateScale(scale: Float) = composed {
    val state = animateFloatAsState(scale)
    this then FastScaleElement({ state.value })
}

// FastRectBackground

private class FastRectBackgroundNode(var backgroundProvider: ContentDrawScope.() -> Color?) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        backgroundProvider()?.let { drawRect(it) }
        drawContent()
    }
}

private data class FastRectBackgroundElement(val backgroundProvider: ContentDrawScope.() -> Color?) : ModifierNodeElement<FastRectBackgroundNode>() {
    override fun create(): FastRectBackgroundNode = FastRectBackgroundNode(backgroundProvider)
    override fun update(node: FastRectBackgroundNode) {
        node.backgroundProvider = backgroundProvider
    }
    override fun InspectorInfo.inspectableProperties() = inspector("fastRectBackground") {
        "backgroundProvider" bind backgroundProvider
    }
}

@Stable
fun Modifier.fastRectBackground(backgroundProvider: ContentDrawScope.() -> Color?) =
    this then FastRectBackgroundElement(backgroundProvider)

@Stable
fun Modifier.fastRectBackground(animatable: Animatable<Color, AnimationVector1D>) =
    this then FastRectBackgroundElement { animatable.value }

@Stable
fun Modifier.fastRectBackground(state: State<Color>) =
    this then FastRectBackgroundElement { state.value }

@Stable
fun Modifier.fastAnimateRectBackground(color: Color) = composed {
    val state = animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = Theme.animation.duration.default, easing = LinearOutSlowInEasing)
    )
    this then FastRectBackgroundElement { state.value }
}

// FastAlpha

private class FastAlphaNode(var alphaProvider: GraphicsLayerScope.() -> Float?) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                alphaProvider()?.let { alpha = it }
            }
        }
    }
}

private data class FastAlphaElement(val alphaProvider: GraphicsLayerScope.() -> Float?) : ModifierNodeElement<FastAlphaNode>() {
    override fun create(): FastAlphaNode = FastAlphaNode(alphaProvider)
    override fun update(node: FastAlphaNode) {
        node.alphaProvider = alphaProvider
    }
    override fun InspectorInfo.inspectableProperties() = inspector("fastAlpha") {
        "alphaProvider" bind alphaProvider
    }
}

@Stable
fun Modifier.fastAlpha(alphaProvider: GraphicsLayerScope.() -> Float?) =
    this then FastAlphaElement(alphaProvider)

@Stable
fun Modifier.fastAlpha(animatable: Animatable<Float, AnimationVector1D>) =
    this then FastAlphaElement { animatable.value }

@Stable
fun Modifier.fastAlpha(state: State<Float>) =
    this then FastAlphaElement { state.value }

@Stable
fun Modifier.fastAnimateAlpha(alpha: Float) = composed {
    val state = animateFloatAsState(
        targetValue = alpha,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 500.0f)
    )
    this then FastAlphaElement { state.value }
}

// fastClip

private class FastClipNode(var shapeProvider: GraphicsLayerScope.() -> Shape?) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                shapeProvider()?.let {
                    shape = it
                    clip = true
                }
            }
        }
    }
}

private data class FastClipElement(val shapeProvider: GraphicsLayerScope.() -> Shape?) : ModifierNodeElement<FastClipNode>() {
    override fun create(): FastClipNode = FastClipNode(shapeProvider)
    override fun update(node: FastClipNode) {
        node.shapeProvider = shapeProvider
    }
    override fun InspectorInfo.inspectableProperties() = inspector("fastClip") {
        "shapeProvider" bind shapeProvider
    }
}

@Stable
fun Modifier.fastClip(shapeProvider: GraphicsLayerScope.() -> Shape?) =
    this then FastClipElement(shapeProvider)


// FastClipCircle

private class FastClipCircleNode : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                shape = CircleShape
                clip = true
            }
        }
    }
}

private data object FastClipCircleElement : ModifierNodeElement<FastClipCircleNode>() {
    override fun create(): FastClipCircleNode = FastClipCircleNode()
    override fun update(node: FastClipCircleNode) { }
    override fun InspectorInfo.inspectableProperties() = inspector("fastClipCircle")
}

@Stable
fun Modifier.fastClipCircle(): Modifier = this then FastClipCircleElement

// FastOffset

private class FastOffsetNode(
    var offsetXProvider: GraphicsLayerScope.() -> Float?,
    var offsetYProvider: GraphicsLayerScope.() -> Float?
) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                offsetXProvider()?.let { translationX = it }
                offsetYProvider()?.let { translationY = it }
            }
        }
    }
}

private data class FastOffsetElement(
    val offsetXProvider: GraphicsLayerScope.() -> Float?,
    val offsetYProvider: GraphicsLayerScope.() -> Float?,
) : ModifierNodeElement<FastOffsetNode>() {
    override fun create(): FastOffsetNode = FastOffsetNode(offsetXProvider, offsetYProvider)
    override fun update(node: FastOffsetNode) {
        node.offsetXProvider = offsetXProvider
        node.offsetYProvider = offsetYProvider
    }
    override fun InspectorInfo.inspectableProperties() = inspector("fastOffset") {
        "offsetXProvider" bind offsetXProvider
        "offsetYProvider" bind offsetYProvider
    }
}

@Stable
fun Modifier.fastOffsetX(offsetProvider: GraphicsLayerScope.() -> Float?) =
    this then FastOffsetElement(offsetProvider, NullFloatProvider)

@JvmName("fastOffsetXByStateFloat")
@Stable
fun Modifier.fastOffsetX(state: State<Float>) =
    this then FastOffsetElement({ state.value }, NullFloatProvider)

@JvmName("fastOffsetXByStateInt")
@Stable
fun Modifier.fastOffsetX(state: State<Int>) =
    this then FastOffsetElement({ state.value.toFloat() }, NullFloatProvider)

@JvmName("fastOffsetXByStateDp")
@Stable
fun Modifier.fastOffsetX(state: State<Dp>) =
    this then FastOffsetElement({ state.value.toPx() }, NullFloatProvider)

@Stable
fun Modifier.fastAnimateOffsetX(offset: Float) = composed {
    val state = animateFloatAsState(targetValue = offset)
    this then FastOffsetElement({ state.value }, NullFloatProvider)
}

@Stable
fun Modifier.fastAnimateOffsetX(offset: Int) = composed {
    val state = animateIntAsState(targetValue = offset)
    this then FastOffsetElement({ state.value.toFloat() }, NullFloatProvider)
}

@Stable
fun Modifier.fastAnimateOffsetX(offset: Dp) = composed {
    val state = animateDpAsState(targetValue = offset)
    this then FastOffsetElement({ state.value.toPx() }, NullFloatProvider)
}

@Stable
fun Modifier.fastOffsetY(offsetProvider: GraphicsLayerScope.() -> Float?) =
    this then FastOffsetElement(NullFloatProvider, offsetProvider)

@JvmName("fastOffsetYByStateFloat")
@Stable
fun Modifier.fastOffsetY(state: State<Float>) =
    this then FastOffsetElement(NullFloatProvider) { state.value }

@JvmName("fastOffsetYByStateInt")
@Stable
fun Modifier.fastOffsetY(state: State<Int>) =
    this then FastOffsetElement(NullFloatProvider) { state.value.toFloat() }

@JvmName("fastOffsetYByStateDp")
@Stable
fun Modifier.fastOffsetY(state: State<Dp>) =
    this then FastOffsetElement(NullFloatProvider) { state.value.toPx() }

@Stable
fun Modifier.fastAnimateOffsetY(offset: Float) = composed {
    val state = animateFloatAsState(targetValue = offset)
    this then FastOffsetElement(NullFloatProvider) { state.value }
}

@Stable
fun Modifier.fastAnimateOffsetY(offset: Int) = composed {
    val state = animateIntAsState(targetValue = offset)
    this then FastOffsetElement(NullFloatProvider) { state.value.toFloat() }
}

@Stable
fun Modifier.fastAnimateOffsetY(offset: Dp) = composed {
    val state = animateDpAsState(targetValue = offset)
    this then FastOffsetElement(NullFloatProvider) { state.value.toPx() }
}

@Stable
fun Modifier.fastOffset(offsetProvider: GraphicsLayerScope.() -> Offset?) =
    this then FastOffsetElement({ offsetProvider()?.x }, { offsetProvider()?.y })

@Stable
fun Modifier.fastOffset(offsetXProvider: GraphicsLayerScope.() -> Float?, offsetYProvider: GraphicsLayerScope.() -> Float?) =
    this then FastOffsetElement(offsetXProvider, offsetYProvider)

@Stable
fun Modifier.fastAnimateOffset(offset: Offset) = composed {
    val state = animateOffsetAsState(offset)
    this then FastOffsetElement({ state.value.x }, { state.value.y })
}