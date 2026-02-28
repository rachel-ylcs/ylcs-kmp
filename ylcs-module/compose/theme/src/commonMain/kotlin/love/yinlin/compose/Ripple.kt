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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

data object Ripple : IndicationNodeFactory {
    const val PRESSED_ALPHA = 0.15f
    const val FOCUSED_ALPHA = 0.12f
    const val DRAGGED_ALPHA = 0.18f
    const val HOVERED_ALPHA = 0.08f
    const val FADEIN_DURATION = 100
    const val RADIUS_DURATION = 250
    const val FADEOUT_DURATION = 175
    const val RIPPLE_EXTRA_RADIUS = 5

    class StateLayer {
        val animatedAlpha = Animatable(0f)
        val interactions: MutableList<Interaction> = mutableListOf()
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
            if (currentInteraction != newInteraction) {
                val defaultTweenSpec = TweenSpec<Float>(durationMillis = 15, easing = LinearEasing)
                if (newInteraction != null) {
                    val targetAlpha = when (newInteraction) {
                        is HoverInteraction.Enter -> HOVERED_ALPHA
                        is FocusInteraction.Focus -> FOCUSED_ALPHA
                        is DragInteraction.Start -> DRAGGED_ALPHA
                        else -> 0f
                    }
                    val incomingAnimationSpec = when (newInteraction) {
                        is HoverInteraction.Enter -> defaultTweenSpec
                        is FocusInteraction.Focus -> TweenSpec(durationMillis = 45, easing = LinearEasing)
                        is DragInteraction.Start -> TweenSpec(durationMillis = 45, easing = LinearEasing)
                        else -> defaultTweenSpec
                    }
                    scope.launch { animatedAlpha.animateTo(targetAlpha, incomingAnimationSpec) }
                } else {
                    val outgoingAnimationSpec = when (currentInteraction) {
                        is HoverInteraction.Enter -> defaultTweenSpec
                        is FocusInteraction.Focus -> defaultTweenSpec
                        is DragInteraction.Start -> TweenSpec(durationMillis = 150, easing = LinearEasing)
                        else -> defaultTweenSpec
                    }
                    scope.launch { animatedAlpha.animateTo(0f, outgoingAnimationSpec) }
                }
                currentInteraction = newInteraction
            }
        }

        fun DrawScope.drawStateLayer(radius: Float, color: Color) {
            val alpha = animatedAlpha.value
            if (alpha > 0f) clipRect { drawCircle(color.copy(alpha = alpha), radius) }
        }
    }

    class RippleAnimation(private var origin: Offset?, private val radius: Float) {
        var startRadius: Float? = null
        var targetCenter: Offset? = null

        val animatedAlpha = Animatable(0f)
        val animatedRadiusPercent = Animatable(0f)
        val animatedCenterPercent = Animatable(0f)
        val finishSignalDeferred = CompletableDeferred<Unit>(null)

        var finishedFadingIn by mutableStateOf(false)
        var finishRequested by mutableStateOf(false)

        suspend fun animate() {
            coroutineScope {
                launch {
                    animatedAlpha.animateTo(1f, tween(durationMillis = FADEIN_DURATION, easing = LinearEasing))
                }
                launch {
                    animatedRadiusPercent.animateTo(1f, tween(durationMillis = RADIUS_DURATION, easing = FastOutSlowInEasing))
                }
                launch {
                    animatedCenterPercent.animateTo(1f, tween(durationMillis = RADIUS_DURATION, easing = LinearEasing))
                }
            }

            finishedFadingIn = true
            finishSignalDeferred.await()

            coroutineScope {
                launch {
                    animatedAlpha.animateTo(0f, tween(durationMillis = FADEOUT_DURATION, easing = LinearEasing))
                }
            }
        }

        fun finish() {
            finishRequested = true
            finishSignalDeferred.complete(Unit)
        }

        fun DrawScope.draw(color: Color) {
            if (startRadius == null) startRadius = max(size.width, size.height) * 0.3f
            if (origin == null) origin = center
            if (targetCenter == null) targetCenter = Offset(size.width / 2.0f, size.height / 2.0f)

            val alpha = if (finishRequested && !finishedFadingIn) 1f else animatedAlpha.value
            val centerOffset = Offset(
                lerp(origin!!.x, targetCenter!!.x, animatedCenterPercent.value),
                lerp(origin!!.y, targetCenter!!.y, animatedCenterPercent.value)
            )

            clipRect {
                drawCircle(
                    color = color.copy(alpha = color.alpha * alpha),
                    radius = lerp(startRadius!!, radius, animatedRadiusPercent.value),
                    center = centerOffset
                )
            }
        }
    }

    class RippleNode(private val interactionSource: InteractionSource) : Modifier.Node(), LayoutAwareModifierNode, CompositionLocalConsumerModifierNode, DrawModifierNode {
        var targetRadius: Float = 0f
        val pendingInteractions = mutableListOf<PressInteraction>()
        val ripples = MutableScatterMap<PressInteraction.Press, RippleAnimation>()
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
                        try {
                            rippleAnimation.animate()
                        } finally {
                            ripples.remove(pressInteraction)
                            invalidateDraw()
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
            targetRadius = with(density) {
                val radiusCoveringBounds = Offset(size.width.toFloat(), size.height.toFloat()).getDistance() / 2f
                radiusCoveringBounds + RIPPLE_EXTRA_RADIUS.dp.toPx()
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
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            val rippleColor = currentValueOf(LocalColor)
            with(stateLayer) { drawStateLayer(targetRadius, rippleColor) }
            ripples.forEach { _, ripple -> with(ripple) { draw(rippleColor.copy(alpha = PRESSED_ALPHA)) } }
        }
    }

    override fun create(interactionSource: InteractionSource): DelegatableNode = object : DelegatingNode() {
        init { delegate(RippleNode(interactionSource)) }
    }
}