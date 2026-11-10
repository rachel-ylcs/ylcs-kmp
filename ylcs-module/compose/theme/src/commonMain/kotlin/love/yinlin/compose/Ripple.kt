package love.yinlin.compose

import androidx.collection.MutableScatterMap
import androidx.compose.animation.core.*
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.node.*
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.extension.cleaning

data object Ripple : IndicationNodeFactory {
    const val PRESSED_ALPHA = 0.15f
    const val FOCUSED_ALPHA = 0.12f
    const val DRAGGED_ALPHA = 0.18f
    const val HOVERED_ALPHA = 0.08f
    const val FADEIN_DURATION = 100
    const val RADIUS_DURATION = 250
    const val FADEOUT_DURATION = 175
    const val RIPPLE_EXTRA_RADIUS = 5

    private val defaultStateLayerSpec = tween<Float>(durationMillis = 15, easing = LinearEasing)
    private val focusAndDragEnterSpec = tween<Float>(durationMillis = 45, easing = LinearEasing)
    private val dragExitSpec = tween<Float>(durationMillis = 150, easing = LinearEasing)
    private val expansionSpec = tween<Float>(durationMillis = RADIUS_DURATION, easing = LinearEasing)
    private val fadeInProgressScale = RADIUS_DURATION.toFloat() / FADEIN_DURATION
    private val fadeOutSpec = tween<Float>(durationMillis = FADEOUT_DURATION, easing = LinearEasing)

    class StateLayer {
        private var alphaAnimation = Animatable(0f)
        val animatedAlpha get() = alphaAnimation
        val interactions: MutableList<Interaction> = []
        var currentInteraction: Interaction? = null

        fun handleInteraction(interaction: Interaction, scope: CoroutineScope) {
            when (interaction) {
                is HoverInteraction.Enter -> interactions.add(interaction)
                is HoverInteraction.Exit -> interactions.remove(interaction.enter)
                is FocusInteraction.Focus -> interactions.add(interaction)
                is FocusInteraction.Unfocus -> interactions.remove(interaction.focus)
                is DragInteraction.Start -> interactions.add(interaction)
                is DragInteraction.Stop -> interactions.remove(interaction.start)
                is DragInteraction.Cancel -> interactions.remove(interaction.start)
                else -> return
            }

            val newInteraction = interactions.lastOrNull()
            if (currentInteraction == newInteraction) return

            val targetAlpha = when (newInteraction) {
                is HoverInteraction.Enter -> HOVERED_ALPHA
                is FocusInteraction.Focus -> FOCUSED_ALPHA
                is DragInteraction.Start -> DRAGGED_ALPHA
                else -> 0f
            }
            val animationSpec = when (newInteraction) {
                is FocusInteraction.Focus, is DragInteraction.Start -> focusAndDragEnterSpec
                null -> if (currentInteraction is DragInteraction.Start) dragExitSpec else defaultStateLayerSpec
                else -> defaultStateLayerSpec
            }
            val alpha = animatedAlpha
            scope.launch { alpha.animateTo(targetAlpha, animationSpec) }
            currentInteraction = newInteraction
        }

        internal fun reset() {
            if (currentInteraction != null || animatedAlpha.isRunning || animatedAlpha.value != 0f) {
                alphaAnimation = Animatable(0f)
            }
            interactions.clear()
            currentInteraction = null
        }

        fun DrawScope.drawStateLayer(radius: Float, color: Color) {
            val alpha = animatedAlpha.value
            if (alpha > 0f) clipRect { drawCircle(color.copy(alpha = alpha), radius) }
        }
    }

    class RippleAnimation(private var origin: Offset?, private val radius: Float) {
        var startRadius: Float? = null
        var targetCenter: Offset? = null

        private val animatedProgress = Animatable(0f)
        val finishSignalDeferred = CompletableDeferred<Unit>(null)

        var finishedFadingIn by mutableStateOf(false)
        var finishRequested by mutableStateOf(false)

        suspend fun animate() {
            animatedProgress.animateTo(1f, expansionSpec)
            finishedFadingIn = true
            finishSignalDeferred.await()
            animatedProgress.animateTo(0f, fadeOutSpec)
        }

        fun finish() {
            finishRequested = true
            finishSignalDeferred.complete(Unit)
        }

        fun DrawScope.draw(color: Color) {
            clipRect { drawUnclipped(color) }
        }

        internal fun DrawScope.drawUnclipped(color: Color) {
            if (startRadius == null) startRadius = maxOf(size.width, size.height) * 0.3f
            if (origin == null) origin = center
            if (targetCenter == null) targetCenter = Offset(size.width / 2.0f, size.height / 2.0f)

            val progress = animatedProgress.value
            val isExpanded = finishedFadingIn
            val expansion = if (isExpanded) 1f else progress
            val alpha = when {
                isExpanded -> progress
                finishRequested -> 1f
                else -> (progress * fadeInProgressScale).coerceAtMost(1f)
            }
            val centerOffset = Offset(
                lerp(origin!!.x, targetCenter!!.x, expansion),
                lerp(origin!!.y, targetCenter!!.y, expansion)
            )

            drawCircle(
                color = color.copy(alpha = color.alpha * alpha),
                radius = lerp(startRadius!!, radius, FastOutSlowInEasing.transform(expansion)),
                center = centerOffset
            )
        }
    }

    class RippleNode(private val interactionSource: InteractionSource) : Modifier.Node(), LayoutAwareModifierNode, CompositionLocalConsumerModifierNode, DrawModifierNode {
        var targetRadius: Float = 0f
        val pendingInteractions: MutableList<PressInteraction> = []
        val ripples = MutableScatterMap<PressInteraction.Press, RippleAnimation>(initialCapacity = 0)
        val stateLayer: StateLayer = StateLayer()
        var hasValidSize: Boolean = false
        var needRedraw: Boolean = true

        override val shouldAutoInvalidate: Boolean = false

        fun handlePressInteraction(pressInteraction: PressInteraction) {
            when (pressInteraction) {
                is PressInteraction.Press -> {
                    ripples.forEach { _, ripple -> ripple.finish() }
                    val origin = pressInteraction.pressPosition
                    val rippleAnimation = RippleAnimation(origin, targetRadius)
                    ripples[pressInteraction] = rippleAnimation
                    coroutineScope.launch {
                        cleaning({
                            rippleAnimation.animate()
                        }) {
                            if (ripples.remove(pressInteraction, rippleAnimation)) invalidateDraw()
                        }
                    }
                    invalidateDraw()
                }
                is PressInteraction.Release -> ripples[pressInteraction.press]?.finish()
                is PressInteraction.Cancel -> ripples[pressInteraction.press]?.finish()
            }
        }

        override fun onRemeasured(size: IntSize) {
            hasValidSize = true
            val density = requireDensity()
            val newTargetRadius = with(density) {
                val radiusCoveringBounds = Offset(size.width.toFloat(), size.height.toFloat()).getDistance() / 2f
                radiusCoveringBounds + RIPPLE_EXTRA_RADIUS.dp.toPx()
            }
            if (targetRadius != newTargetRadius) {
                targetRadius = newTargetRadius
                if (stateLayer.animatedAlpha.value > 0f) invalidateDraw()
            }
            pendingInteractions.fastForEach { handlePressInteraction(it) }
            pendingInteractions.clear()
        }

        override fun onAttach() {
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction -> {
                            if (hasValidSize) handlePressInteraction(interaction)
                            else pendingInteractions += interaction
                        }
                        else -> {
                            if (needRedraw) {
                                invalidateDraw()
                                needRedraw = false
                            }
                            stateLayer.handleInteraction(interaction, this)
                        }
                    }
                }
            }
        }

        override fun onDetach() {
            ripples.clear()
            stateLayer.reset()
            pendingInteractions.clear()
            targetRadius = 0f
            hasValidSize = false
            needRedraw = true
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            // Observe alpha even when idle so a state-layer animation can invalidate drawing.
            val stateAlpha = stateLayer.animatedAlpha.value
            val hasRipples = ripples.isNotEmpty()
            if (stateAlpha <= 0f && !hasRipples) return

            val rippleColor = currentValueOf(LocalColor)
            clipRect {
                if (stateAlpha > 0f) drawCircle(rippleColor.copy(alpha = stateAlpha), targetRadius)
                if (hasRipples) {
                    val pressedColor = rippleColor.copy(alpha = PRESSED_ALPHA)
                    ripples.forEach { _, ripple -> with(ripple) { drawUnclipped(pressedColor) } }
                }
            }
        }
    }

    override fun create(interactionSource: InteractionSource): DelegatableNode = RippleNode(interactionSource)
}