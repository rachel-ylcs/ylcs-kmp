package love.yinlin.compose.ui.node

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import love.yinlin.compose.Colors
import love.yinlin.compose.extension.translate
import love.yinlin.compose.image.input.ImageSource
import love.yinlin.compose.platform.inspector
import love.yinlin.compose.ui.image.CropImageState
import love.yinlin.compose.ui.image.ImageState
import love.yinlin.extension.then

@Stable
private enum class CropImageHandle {
    TopLeft, TopRight, BottomLeft, BottomRight, Inside;
}

private fun DrawScope.drawCorner(
    rect: Rect,
    cornerLength: Float,
    frameColor: Color,
    frameAlpha: Float
) {
    val width = 2.dp.toPx()
    val offsetFromVertex = width * 2f
    val offsetLine = width / 2f

    rect.topLeft.translate(offsetFromVertex, offsetFromVertex).then { start ->
        drawLine(
            start = start.translate(x = -offsetLine),
            end = start.translate(x = cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
        drawLine(
            start = start,
            end = start.translate(y = cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
    }

    rect.topRight.translate(-offsetFromVertex, offsetFromVertex).then { start ->
        drawLine(
            start = start.translate(x = offsetLine),
            end = start.translate(x = -cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
        drawLine(
            start = start,
            end = start.translate(y = cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
    }

    rect.bottomLeft.translate(offsetFromVertex, -offsetFromVertex).then { start ->
        drawLine(
            start = start.translate(x = -offsetLine),
            end = start.translate(x = cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
        drawLine(
            start = start,
            end = start.translate(y = -cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
    }

    rect.bottomRight.translate(-offsetFromVertex, -offsetFromVertex).then { start ->
        drawLine(
            start = start.translate(x = offsetLine),
            end = start.translate(x = -cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
        drawLine(
            start = start,
            end = start.translate(y = -cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
    }
}

private fun DrawScope.drawGrid(
    rect: Rect,
    gridColor: Color,
    gridAlpha: Float
) {
    val width = 1.dp.toPx()
    val width13 = rect.size.width / 3
    val width23 = width13 * 2

    drawLine(
        color = gridColor,
        alpha = gridAlpha,
        start = rect.topLeft.translate(x = width13),
        end = rect.bottomLeft.translate(x = width13),
        strokeWidth = width
    )
    drawLine(
        color = gridColor,
        alpha = gridAlpha,
        start = rect.topLeft.translate(x = width23),
        end = rect.bottomLeft.translate(x = width23),
        strokeWidth = width
    )

    val height13 = rect.size.height / 3
    val height23 = height13 * 2

    drawLine(
        color = gridColor,
        alpha = gridAlpha,
        start = rect.topLeft.translate(y = height13),
        end = rect.topRight.translate(y = height13),
        strokeWidth = width
    )
    drawLine(
        color = gridColor,
        alpha = gridAlpha,
        start = rect.topLeft.translate(y = height23),
        end = rect.topRight.translate(y = height23),
        strokeWidth = width
    )
}

private class CropImageRegionNode(
    private var imageState: ImageState,
    private var cropImageState: CropImageState,
    private var aspectRatio: Float,
) : DelegatingNode(), DrawModifierNode, PointerInputModifierNode, LayoutAwareModifierNode,
    CompositionLocalConsumerModifierNode, ObserverModifierNode {
    private var viewport = IntSize.Zero
    private var imageBounds = Rect.Zero
    private var visibleBounds = Rect.Zero
    private var imageViewport = IntSize.Zero
    private var originalSize: IntSize? = null
    private var source: ImageSource? = null
    private var version: String? = null
    private var resetGeneration = -1
    private var displayRatio = 0f
    private var tolerance = 0f
    private var geometryReady = false
    private var resetPending = true
    private var handle: CropImageHandle? = null
    private val maskPaint = Paint()

    private val pointerInputNode = delegate(SuspendingPointerInputModifierNode {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)

            if (!geometryReady || cropImageState.frameRect.isEmpty) return@awaitEachGesture

            handle = hitTest(down.position) ?: return@awaitEachGesture

            val start = awaitTouchSlopOrCancellation(down.id) { change, amount ->
                moveFrame(amount)
                change.consume()
            }

            if (start != null) {
                drag(start.id) { change ->
                    moveFrame(change.positionChange())
                    change.consume()
                }
            }

            handle = null
        }
    })

    override fun onAttach() = updateGeometry()

    override fun onRemeasured(size: IntSize) {
        if (viewport != size) {
            viewport = size
            updateGeometry()
        }
    }

    override fun onObservedReadsChanged() = updateGeometry()

    override fun onDetach() {
        handle = null
        geometryReady = false
    }

    override fun onReset() {
        handle = null
        geometryReady = false
        resetPending = true
        if (isAttached) updateGeometry()
    }

    fun update(imageState: ImageState, cropImageState: CropImageState, aspectRatio: Float) {
        val reset = this.imageState !== imageState || this.cropImageState !== cropImageState || this.aspectRatio != aspectRatio
        this.imageState = imageState
        this.cropImageState = cropImageState
        this.aspectRatio = aspectRatio
        if (isAttached) updateGeometry(reset)
    }

    private fun updateGeometry(reset: Boolean = false) {
        if (!isAttached) return

        val oldSource = source
        val oldVersion = version
        val oldReset = resetGeneration
        val oldBounds = imageBounds
        val oldRatio = displayRatio
        val region = Snapshot.withoutReadObservation { cropImageState.result }
        val hadFrame = region.widthPercent > 0f && region.heightPercent > 0f

        // zoom 条件下禁止 crop
        var zoomEnabled = false
        observeReads {
            zoomEnabled = imageState.zoomEnabled
            if (!zoomEnabled) {
                imageBounds = imageState.contentRect
                imageViewport = imageState.contentViewport
                originalSize = imageState.originalSize
                source = imageState.source
                version = imageState.version
                resetGeneration = cropImageState.resetGeneration
                tolerance = with(currentValueOf(LocalDensity)) { 24.dp.toPx() }
            }
        }

        if (zoomEnabled) {
            handle = null
            geometryReady = false
            resetPending = true
            imageBounds = Rect.Zero
            visibleBounds = Rect.Zero
            cropImageState.imageRect = Rect.Zero
            cropImageState.frameRect = Rect.Zero
            pointerInputNode.resetPointerInputHandler()
            invalidateDraw()
            return
        }

        resetPending = resetPending || reset || oldSource != source || oldVersion != version || oldReset != resetGeneration
        geometryReady = imageViewport == viewport && !imageBounds.isEmpty && viewport.width > 0 && viewport.height > 0

        if (imageBounds.isEmpty) {
            handle = null
            cropImageState.imageRect = Rect.Zero
            cropImageState.frameRect = Rect.Zero
            invalidateDraw()
            return
        }

        if (!geometryReady) {
            invalidateDraw()
            return
        }

        visibleBounds = imageBounds.intersect(Rect(Offset.Zero, viewport.toSize()))

        if (visibleBounds.isEmpty) {
            geometryReady = false
            cropImageState.imageRect = Rect.Zero
            cropImageState.frameRect = Rect.Zero
            invalidateDraw()
            return
        }

        val original = originalSize
        displayRatio = if (aspectRatio == 0f || original == null) 0f else (aspectRatio.toDouble() * imageBounds.width * original.height / (imageBounds.height.toDouble() * original.width)).toFloat()

        val initialize = resetPending || !hadFrame
        cropImageState.imageRect = imageBounds
        cropImageState.frameRect = if (initialize) initialFrame() else fitFrame((region * imageBounds.size).translate(imageBounds.topLeft))

        if (initialize || oldBounds != imageBounds || oldRatio != displayRatio) {
            handle = null
            pointerInputNode.resetPointerInputHandler()
        }
        resetPending = false
        invalidateDraw()
    }

    private fun initialFrame(): Rect {
        val available = minOf(visibleBounds.width, visibleBounds.height) * 0.8f
        val width: Float
        val height: Float

        if (displayRatio == 0f) {
            width = visibleBounds.width * 0.8f
            height = visibleBounds.height * 0.8f
        }
        else if (displayRatio >= 1f) {
            width = available
            height = available / displayRatio
        }
        else {
            width = available * displayRatio
            height = available
        }

        return Rect(visibleBounds.center - Offset(width / 2f, height / 2f), Size(width, height))
    }

    private fun fitFrame(frame: Rect): Rect {
        val factor = minOf(1f, visibleBounds.width / frame.width, visibleBounds.height / frame.height)
        val width = frame.width * factor
        val height = frame.height * factor
        val left = frame.left.coerceIn(visibleBounds.left, maxOf(visibleBounds.left, visibleBounds.right - width))
        val top = frame.top.coerceIn(visibleBounds.top, maxOf(visibleBounds.top, visibleBounds.bottom - height))

        return Rect(left, top, left + width, top + height)
    }

    private fun hitTest(position: Offset): CropImageHandle? = with(cropImageState.frameRect) {
        val radius = minOf(tolerance, width / 3f, height / 3f)

        when {
            Rect(topLeft, radius).contains(position) -> CropImageHandle.TopLeft
            Rect(topRight, radius).contains(position) -> CropImageHandle.TopRight
            Rect(bottomLeft, radius).contains(position) -> CropImageHandle.BottomLeft
            Rect(bottomRight, radius).contains(position) -> CropImageHandle.BottomRight
            contains(position) -> CropImageHandle.Inside
            else -> null
        }
    }

    private fun moveFrame(amount: Offset) {
        val selected = handle ?: return
        if (!geometryReady) return

        cropImageState.frameRect = if (selected == CropImageHandle.Inside) fitFrame(cropImageState.frameRect.translate(amount)) else resizeFrame(selected, amount)
    }

    private fun resizeFrame(handle: CropImageHandle, amount: Offset): Rect {
        val frame = cropImageState.frameRect
        val left = handle == CropImageHandle.TopLeft || handle == CropImageHandle.BottomLeft
        val top = handle == CropImageHandle.TopLeft || handle == CropImageHandle.TopRight
        val anchor = Offset(if (left) frame.right else frame.left, if (top) frame.bottom else frame.top)
        val maxWidth = (if (left) anchor.x - visibleBounds.left else visibleBounds.right - anchor.x).coerceAtLeast(0f)
        val maxHeight = (if (top) anchor.y - visibleBounds.top else visibleBounds.bottom - anchor.y).coerceAtLeast(0f)
        val wantedWidth = frame.width + if (left) -amount.x else amount.x
        val wantedHeight = frame.height + if (top) -amount.y else amount.y
        val minimum = tolerance * 2f
        val width: Float
        val height: Float

        if (displayRatio == 0f) {
            width = wantedWidth.coerceIn(minOf(minimum, maxWidth), maxWidth)
            height = wantedHeight.coerceIn(minOf(minimum, maxHeight), maxHeight)
        }
        else {
            val ratio = displayRatio.toDouble()
            val desired = ((wantedWidth * ratio + wantedHeight) / (ratio * ratio + 1.0)).toFloat()
            val maximum = minOf(maxHeight, maxWidth / displayRatio)
            val minimumHeight = minOf(maximum, maxOf(minimum, minimum / displayRatio))
            height = desired.coerceIn(minimumHeight, maximum)
            width = height * displayRatio
        }

        return Rect(
            left = if (left) anchor.x - width else anchor.x,
            top = if (top) anchor.y - height else anchor.y,
            right = if (left) anchor.x else anchor.x + width,
            bottom = if (top) anchor.y else anchor.y + height
        )
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        if (!geometryReady || imageState.contentRect.isEmpty || cropImageState.frameRect.isEmpty) return
        val frame = cropImageState.frameRect

        clipRect {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), maskPaint)
                drawRect(color = Colors.Black, alpha = 0.5f)
                drawRect(color = Colors.Transparent, topLeft = frame.topLeft, size = frame.size, blendMode = BlendMode.SrcOut)
                canvas.restore()
            }
            drawRect(color = Colors.White, alpha = 0.8f, topLeft = frame.topLeft, size = frame.size, style = Stroke(2.dp.toPx()))
            drawCorner(frame, tolerance / 2f, Colors.White, 0.8f)
            drawGrid(frame, Colors.White, 0.6f)
        }
    }

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) =
        pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)

    override fun onCancelPointerInput() {
        handle = null
        pointerInputNode.onCancelPointerInput()
    }
}

private data class CropImageRegionElement(
    val imageState: ImageState,
    val cropImageState: CropImageState,
    val aspectRatio: Float,
) : ModifierNodeElement<CropImageRegionNode>() {
    override fun create(): CropImageRegionNode = CropImageRegionNode(imageState, cropImageState, aspectRatio)
    override fun update(node: CropImageRegionNode) = node.update(imageState, cropImageState, aspectRatio)
    override fun InspectorInfo.inspectableProperties() = inspector("cropImageRegion") {
        "imageState" bind imageState
        "cropImageState" bind cropImageState
        "aspectRatio" bind aspectRatio
    }
}

/**
 * 为关联的图片添加区域裁剪交互。
 *
 * 对应 BasicImage 开启 ImageOptions.zoom 时，裁剪交互停用。
 *
 * 不绘制裁剪层、不处理裁剪手势，并清空裁剪范围。
 *
 * 关闭 zoom 后重新初始化裁剪框。
 */
@Stable
fun Modifier.cropImageRegion(imageState: ImageState, cropImageState: CropImageState, aspectRatio: Float = 0f): Modifier {
    require(aspectRatio.isFinite() && aspectRatio >= 0f)
    return this then CropImageRegionElement(imageState, cropImageState, aspectRatio)
}
