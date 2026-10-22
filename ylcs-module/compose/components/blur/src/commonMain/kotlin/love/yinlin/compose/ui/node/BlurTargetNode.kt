// Copyright 2023-2026, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for Rachel: same-root capture, uniform blur, noise and a single color tint.

package love.yinlin.compose.ui.node

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.max
import kotlin.math.min

internal class BlurTargetNode(private var state: BlurState, private var parameters: BlurParameters) : BlurNode(), GlobalPositionAwareModifierNode, LayoutAwareModifierNode {
    private var contentLayer: GraphicsLayer? = null
    private var capturedSource: GraphicsLayer? = null
    private var capturedVersion: Long = -1L
    private var capturedBounds: Rect? = null
    private var capturedTargetSize: Size = Size.Unspecified
    private var capturedScale: Float = Float.NaN
    private var capturedBackground: Color = Color.Unspecified
    private var hasCapture = false

    private val sourceTransform = Matrix()
    private val rootTransform = Matrix()
    private val capturedTransform = Matrix()

    private var renderer: BlurRenderer? = null
    private var renderEffect: RenderEffect? = null
    private var effectRadius: Float = Float.NaN
    private var effectTint: Color = Color.Unspecified
    private var effectNoise: Float = Float.NaN
    private var effectScale: Float = Float.NaN

    internal fun update(newState: BlurState, newParameters: BlurParameters) {
        if (state !== newState) {
            if (isAttached) state.detachTarget(this)
            releaseResources()
            state = newState
            if (isAttached) state.attachTarget(this)
        }
        parameters = newParameters
        invalidateDraw()
    }

    override fun attachBlur() {
        state.attachTarget(this)
    }

    override fun detachBlur() {
        state.detachTarget(this)
    }

    internal fun invalidateSource() {
        if (isAttached) invalidateDraw()
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        invalidateDraw()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        if (size.width <= 0f || size.height <= 0f) {
            releaseResources()
            drawContent()
            return
        }

        val source = state.source
        if (!supportsBlur()) {
            releaseResources()
            if (source != null) drawRect(parameters.fallbackTint)
            drawContent()
            return
        }

        if (source?.isRecording == true) {
            drawContent()
            return
        }

        val sourceLayer = source?.contentLayer?.takeUnless { it.isReleased }
        if (source != null && sourceLayer != null && sourceLayer.size.width > 0 && sourceLayer.size.height > 0) {
            capture(source, sourceLayer)
        }

        val layer = contentLayer?.takeUnless { it.isReleased }
        val bounds = capturedBounds
        if (hasCapture && layer != null && bounds != null && capturedTargetSize == size) {
            val inputScale = parameters.inputScale
            if (layer.size == (bounds.size * inputScale).roundToIntSize()) {
                updateEffect(layer, inputScale)
                layer.renderEffect = renderEffect
                layer.clip = true
                val scaledSize = size * inputScale
                val outputScale = max(size.width / scaledSize.width, size.height / scaledSize.height)
                clipRect {
                    translate(bounds.left, bounds.top) {
                        scale(outputScale, pivot = Offset.Zero) {
                            drawLayer(layer)
                        }
                    }
                }
            }
        }
        drawContent()
    }

    private fun ContentDrawScope.capture(source: BlurSourceNode, sourceLayer: GraphicsLayer) {
        val targetCoordinates = coordinates?.takeIf { it.isAttached } ?: return
        val sourceCoordinates = source.coordinates?.takeIf { it.isAttached } ?: return
        val rootCoordinates = targetCoordinates.findRootCoordinates()
        check(rootCoordinates === sourceCoordinates.findRootCoordinates()) {
            "blurSource and blurTarget must belong to the same Compose layout root."
        }

        sourceTransform.reset()
        targetCoordinates.transformFrom(sourceCoordinates, sourceTransform)
        val sourceBounds = sourceTransform.map(Rect(Offset.Zero, sourceCoordinates.size.toSize()))

        rootTransform.reset()
        targetCoordinates.transformFrom(rootCoordinates, rootTransform)
        (val left, val top, val right, val bottom) = rootTransform.map(Rect(Offset.Zero, rootCoordinates.size.toSize()))
        val allowedBounds = Rect(
            min(left, sourceBounds.left),
            min(top, sourceBounds.top),
            max(right, sourceBounds.right),
            max(bottom, sourceBounds.bottom),
        )

        val radiusPx = parameters.blurRadius.toPx()
        var bounds = Rect(Offset.Zero, size)
        if (radiusPx >= 1f) bounds = bounds.inflate(radiusPx)
        if (parameters.clipToSource) bounds = bounds.intersect(sourceBounds)
        bounds = bounds.intersect(allowedBounds)

        val inputScale = parameters.inputScale
        val scaledSize = (bounds.size * inputScale).roundToIntSize()
        if (scaledSize.width <= 0 || scaledSize.height <= 0) {
            releaseResources()
            return
        }

        val context = graphicsContext ?: return
        val layer = contentLayer?.takeUnless { it.isReleased } ?: context.createGraphicsLayer().also {
            contentLayer = it
            hasCapture = false
        }

        val needsCapture = !hasCapture || capturedSource !== sourceLayer ||
            capturedVersion != source.contentVersion || capturedBounds != bounds ||
            capturedTargetSize != size || capturedScale != inputScale ||
            capturedBackground != parameters.backgroundColor ||
            !capturedTransform.values.contentEquals(sourceTransform.values)

        if (needsCapture) {
            layer.renderEffect = null
            layer.alpha = 1f
            layer.record(size = scaledSize) {
                drawRect(parameters.backgroundColor)
                scale(inputScale, pivot = Offset.Zero) {
                    translate(-bounds.left, -bounds.top) {
                        withTransform({ transform(sourceTransform) }) {
                            drawLayer(sourceLayer)
                        }
                    }
                }
            }
            layer.setRectOutline(size = scaledSize.toSize())

            capturedSource = sourceLayer
            capturedVersion = source.contentVersion
            capturedBounds = bounds
            capturedTargetSize = this@capture.size
            capturedScale = inputScale
            capturedBackground = parameters.backgroundColor
            capturedTransform.setFrom(sourceTransform)
            hasCapture = true
        }
    }

    private fun ContentDrawScope.updateEffect(layer: GraphicsLayer, inputScale: Float) {
        val radiusPx = (parameters.blurRadius * inputScale).toPx()
        val tint = parameters.effectTint
        if (renderer == null || effectRadius != radiusPx || effectTint != tint ||
            effectNoise != parameters.noiseFactor || effectScale != inputScale) {
            layer.renderEffect = null
            val currentRenderer = renderer ?: createBlurRenderer(this@BlurTargetNode).also { renderer = it }
            renderEffect = currentRenderer.createEffect(radiusPx, tint, parameters.noiseFactor, inputScale)
            effectRadius = radiusPx
            effectTint = tint
            effectNoise = parameters.noiseFactor
            effectScale = inputScale
        }
    }

    override fun releaseResources() {
        val layer = contentLayer
        contentLayer = null
        if (layer != null && !layer.isReleased) {
            layer.renderEffect = null
            graphicsContext?.releaseGraphicsLayer(layer)
        }
        renderer?.release()
        renderer = null
        renderEffect = null
        effectRadius = Float.NaN
        effectTint = Color.Unspecified
        effectNoise = Float.NaN
        effectScale = Float.NaN
        capturedSource = null
        capturedVersion = -1L
        capturedBounds = null
        capturedTargetSize = Size.Unspecified
        capturedScale = Float.NaN
        capturedBackground = Color.Unspecified
        hasCapture = false
    }
}
