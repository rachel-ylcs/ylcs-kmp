// Copyright 2023-2026, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for Rachel: a single, same-root source and demand-driven capture.

package love.yinlin.compose.ui.node

import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.node.updateLayerBlock
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import love.yinlin.extension.cleaning
import kotlin.math.roundToInt

internal class BlurSourceNode(private var state: BlurState) : BlurNode(), GlobalPositionAwareModifierNode, LayoutAwareModifierNode, LayoutModifierNode {
    internal var contentLayer: GraphicsLayer? = null
        private set

    internal var contentVersion: Long = 0L
        private set

    internal var isRecording: Boolean = false
        private set

    private var captureDemand = false
    private var refreshJob: Job? = null
    private var snapshotObserver: ObserverHandle? = null

    internal fun update(newState: BlurState) {
        if (state === newState) return
        if (isAttached) state.detachSource(this)
        releaseResources()
        state = newState
        if (isAttached) {
            state.attachSource(this)
            invalidatePlacement()
            invalidateDraw()
        }
    }

    override fun attachBlur() {
        state.attachSource(this)
    }

    override fun detachBlur() {
        stopInvalidationObserver()
        state.detachSource(this)
    }

    internal fun updateCaptureDemand() {
        val newDemand = state.hasTargets
        if (captureDemand != newDemand) {
            captureDemand = newDemand
            if (!newDemand) releaseResources()
            invalidatePlacement()
            invalidateDraw()
        }
        updateInvalidationObserver()
    }

    private fun updateInvalidationObserver() {
        if (!captureDemand || !needsBlurInvalidationWorkaround()) {
            stopInvalidationObserver()
        }
        else if (snapshotObserver == null) {
            val scope = coroutineScope
            snapshotObserver = Snapshot.registerApplyObserver { _, _ ->
                scope.launch {
                    if (isAttached && captureDemand && !hostStopped) scheduleRefresh()
                }
            }
        }
    }

    private fun scheduleRefresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = coroutineScope.launch {
            withFrameNanos { }
            if (isAttached && captureDemand && !hostStopped) {
                contentVersion++
                state.invalidateTargets()
            }
        }
    }

    private fun stopInvalidationObserver() {
        snapshotObserver?.dispose()
        snapshotObserver = null
        refreshJob?.cancel()
        refreshJob = null
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            if (captureDemand) placeable.placeWithLayer(0, 0)
            else placeable.place(0, 0)
        }
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        state.invalidateTargets()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        state.invalidateTargets()
    }

    override fun ContentDrawScope.draw() {
        if (!captureDemand || hostStopped || !supportsBlur() || size.minDimension.roundToInt() < 1) {
            releaseCapture()
            drawContent()
            return
        }

        val context = graphicsContext
        if (context == null) {
            drawContent()
            return
        }
        val layer = contentLayer?.takeUnless { it.isReleased } ?: context.createGraphicsLayer().also { contentLayer = it }

        isRecording = true
        cleaning({
            layer.record {
                this@draw.drawContent()
            }
        }) {
            isRecording = false
        }
        contentVersion++
        drawLayer(layer)
        state.invalidateTargets()

        if (needsBlurInvalidationWorkaround()) scheduleRefresh()
    }

    private fun releaseCapture() {
        val layer = contentLayer ?: return
        contentLayer = null
        if (!layer.isReleased) graphicsContext?.releaseGraphicsLayer(layer)
        contentVersion++
        state.invalidateTargets()
    }

    override fun releaseResources() {
        releaseCapture()
        if (hostStopped) stopInvalidationObserver()
    }

    override fun resetBlur() {
        updateLayerBlock(null)
        invalidatePlacement()
    }

    override fun onHostStarted() {
        updateInvalidationObserver()
        invalidatePlacement()
    }
}
