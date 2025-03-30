package love.yinlin.ui.component.image

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import kotlinx.coroutines.launch
import love.yinlin.common.Colors
import love.yinlin.extension.rememberState
import love.yinlin.platform.Coroutines
import love.yinlin.platform.OS
import kotlin.math.max
import kotlin.math.min

@Stable
class CropState(
    bitmap: ImageBitmap? = null,
    aspectRatio: Float = 0f
) {
    var bitmap: ImageBitmap? by mutableStateOf(bitmap)
    var aspectRatio: Float by mutableFloatStateOf(aspectRatio)
    internal var frameRect: Rect by mutableStateOf(Rect.Zero)
    internal var imageRect: Rect by mutableStateOf(Rect.Zero)

    private fun Float.adjustLeft(minimumVertexDistance: Float) =
        coerceAtLeast(imageRect.left).coerceAtMost(frameRect.right - minimumVertexDistance)

    private fun Float.adjustTop(minimumVertexDistance: Float) =
        coerceAtLeast(imageRect.top).coerceAtMost(frameRect.bottom - minimumVertexDistance)

    private fun Float.adjustRight(minimumVertexDistance: Float) =
        coerceAtMost(imageRect.right).coerceAtLeast(frameRect.left + minimumVertexDistance)

    private fun Float.adjustBottom(minimumVertexDistance: Float) =
        coerceAtMost(imageRect.bottom).coerceAtLeast(frameRect.top + minimumVertexDistance)

    private fun scaleFlexibleRect(
        point: TouchRegion.Vertex,
        amount: Offset,
        minimumVertexDistance: Float
    ): Rect = frameRect.run {
        val newLeft = (left + amount.x).adjustLeft(minimumVertexDistance)
        val newTop = (top + amount.y).adjustTop(minimumVertexDistance)
        val newRight = (right + amount.x).adjustRight(minimumVertexDistance)
        val newBottom = (bottom + amount.y).adjustBottom(minimumVertexDistance)
        when (point) {
            TouchRegion.Vertex.TOP_LEFT -> Rect(newLeft, newTop, right, bottom)
            TouchRegion.Vertex.TOP_RIGHT -> Rect(left, newTop, newRight, bottom)
            TouchRegion.Vertex.BOTTOM_LEFT -> Rect(newLeft, top, right, newBottom)
            TouchRegion.Vertex.BOTTOM_RIGHT -> Rect(left, top, newRight, newBottom)
        }
    }

    private fun scaleAspectRatioRect(
        point: TouchRegion.Vertex,
        amount: Offset,
        minimumVertexDistance: Float
    ): Rect = frameRect.run {
        val (a, b) = when (point) {
            TouchRegion.Vertex.TOP_LEFT, TouchRegion.Vertex.BOTTOM_RIGHT -> (bottom - top) / (right - left) to (right * top - left * bottom) / (right - left)
            else -> (top - bottom) / (right - left) to (right * bottom - left * top) / (right - left)
        }
        val calculateY = { x: Float -> a * x + b }
        val calculateX = { y: Float -> (y - b) / a }
        when (point) {
            TouchRegion.Vertex.TOP_LEFT -> {
                val rLeft = (left + amount.x).adjustLeft(minimumVertexDistance)
                val rTop = calculateY(rLeft).adjustTop(minimumVertexDistance)
                copy(left = calculateX(rTop), top = rTop)
            }
            TouchRegion.Vertex.TOP_RIGHT -> {
                val rRight = (right + amount.x).adjustRight(minimumVertexDistance)
                val rTop = calculateY(rRight).adjustTop(minimumVertexDistance)
                copy(right = calculateX(rTop), top = rTop)
            }
            TouchRegion.Vertex.BOTTOM_LEFT -> {
                val rLeft = (left + amount.x).adjustLeft(minimumVertexDistance)
                val rBottom = calculateY(rLeft).adjustBottom(minimumVertexDistance)
                copy(left = calculateX(rBottom), bottom = rBottom)
            }
            TouchRegion.Vertex.BOTTOM_RIGHT -> {
                val rRight = (right + amount.x).adjustRight(minimumVertexDistance)
                val rBottom = calculateY(rRight).adjustBottom(minimumVertexDistance)
                copy(right = calculateX(rBottom), bottom = rBottom)
            }
        }
    }

    internal fun scaleFrameRect(
        point: TouchRegion.Vertex,
        amount: Offset,
        minimumVertexDistance: Float
    ) {
        frameRect = frameRect.run {
            if (aspectRatio == 0f) scaleFlexibleRect(
                point = point,
                amount = amount,
                minimumVertexDistance = minimumVertexDistance
            ) else scaleAspectRatioRect(
                point = point,
                amount = amount,
                minimumVertexDistance = minimumVertexDistance
            )
        }
    }

    internal fun translateFrameRect(offset: Offset) {
        var newRect = frameRect.translate(offset)
        if (newRect.left < imageRect.left) newRect = newRect.translate(imageRect.left - newRect.left, 0f)
        if (newRect.right > imageRect.right) newRect = newRect.translate(imageRect.right - newRect.right, 0f)
        if (newRect.top < imageRect.top) newRect = newRect.translate(0f, imageRect.top - newRect.top)
        if (newRect.bottom > imageRect.bottom) newRect = newRect.translate(0f, imageRect.bottom - newRect.bottom)
        frameRect = newRect
    }

    internal suspend fun cropImage(): ImageBitmap? = Coroutines.io {
        bitmap?.let {
            val scale = it.width / imageRect.width
            OS.Image.crop(
                bitmap = it,
                startX = ((frameRect.left - imageRect.left) * scale).toInt(),
                startY = ((frameRect.top - imageRect.top) * scale).toInt(),
                width = (frameRect.width * scale).toInt().coerceIn(1 .. it.width),
                height = (frameRect.height * scale).toInt().coerceIn(1 .. it.height)
            )
        }
    }
}

internal sealed interface TouchRegion {
    enum class Vertex : TouchRegion {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
    data object Inside : TouchRegion
}

private fun Offset.translateX(amount: Float) = copy(x = x + amount)
private fun Offset.translateY(amount: Float) = copy(y = y + amount)
private fun Offset.translate(amountX: Float, amountY: Float) = copy(x = x + amountX, y = y + amountY)

private fun DrawScope.drawCorner(
    rect: Rect,
    cornerLength: Float,
    frameColor: Color,
    frameAlpha: Float
) {
    val width = 2.dp.toPx() // option.frameWidth.toPx()
    val offsetFromVertex = 4.dp.toPx()
    rect.topLeft.translate(offsetFromVertex, offsetFromVertex).let { start ->
        drawLine(
            start = start.translateX(-width / 2),
            end = start.translateX(cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
        drawLine(
            start = start,
            end = start.translateY(cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
    }
    rect.topRight.translate(-offsetFromVertex, offsetFromVertex).let { start ->
        drawLine(
            start = start.translateX(width / 2),
            end = start.translateX(-cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
        drawLine(
            start = start,
            end = start.translateY(cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
    }
    rect.bottomLeft.translate(offsetFromVertex, -offsetFromVertex).let { start ->
        drawLine(
            start = start.translateX(-width / 2),
            end = start.translateX(cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
        drawLine(
            start = start,
            end = start.translateY(-cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
    }
    rect.bottomRight.translate(-offsetFromVertex, -offsetFromVertex).let { start ->
        drawLine(
            start = start.translateX(width / 2),
            end = start.translateX(-cornerLength),
            color = frameColor,
            alpha = frameAlpha,
            strokeWidth = width
        )
        drawLine(
            start = start,
            end = start.translateY(-cornerLength),
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
    val width = 1.dp.toPx() // option.gridWidth
    val widthOneThird = rect.size.width / 3
    val widthTwoThird = widthOneThird * 2
    drawLine(
        color = gridColor,
        alpha = gridAlpha,
        start = rect.topLeft.translateX(widthOneThird),
        end = rect.bottomLeft.translateX(widthOneThird),
        strokeWidth = width
    )
    drawLine(
        color = gridColor,
        alpha = gridAlpha,
        start = rect.topLeft.translateX(widthTwoThird),
        end = rect.bottomLeft.translateX(widthTwoThird),
        strokeWidth = width
    )
    val heightOneThird = rect.size.height / 3
    val heightTwoThird = heightOneThird * 2
    drawLine(
        color = gridColor,
        alpha = gridAlpha,
        start = rect.topLeft.translateY(heightOneThird),
        end = rect.topRight.translateY(heightOneThird),
        strokeWidth = width
    )
    drawLine(
        color = gridColor,
        alpha = gridAlpha,
        start = rect.topLeft.translateY(heightTwoThird),
        end = rect.topRight.translateY(heightTwoThird),
        strokeWidth = width
    )
}

@Composable
fun CropImage(
    state: CropState,
    onCropped: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val tolerance = with(LocalDensity.current) { 24.dp.toPx() }
    var touchRegion: TouchRegion? by rememberState { null }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier.pointerInput(state.bitmap, state.aspectRatio) {
            if (state.bitmap != null) {
                detectDragGestures(
                    onDragStart = { position ->
                        touchRegion = state.frameRect.run { when {
                            Rect(topLeft, tolerance).contains(position) -> TouchRegion.Vertex.TOP_LEFT
                            Rect(topRight, tolerance).contains(position) -> TouchRegion.Vertex.TOP_RIGHT
                            Rect(bottomLeft, tolerance).contains(position) -> TouchRegion.Vertex.BOTTOM_LEFT
                            Rect(bottomRight, tolerance).contains(position) -> TouchRegion.Vertex.BOTTOM_RIGHT
                            contains(position) -> TouchRegion.Inside
                            else -> null
                        } }
                    },
                    onDragEnd = {
                        touchRegion = null
                    }
                ) { change, dragAmount ->
                    touchRegion?.let {
                        when (it) {
                            is TouchRegion.Vertex -> state.scaleFrameRect(it, dragAmount, tolerance * 2)
                            TouchRegion.Inside -> state.translateFrameRect(dragAmount)
                        }
                        change.consume()
                    }
                }
            }
        }
    ) {
        // Image
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(Colors.Black) // option.background
            state.bitmap?.let { bitmap ->
                drawImage(
                    image = bitmap,
                    dstSize = state.imageRect.size.toIntSize(),
                    dstOffset = state.imageRect.topLeft.run { IntOffset(x.toInt(), y.toInt()) }
                )
            }
        }

        // Overlay
        state.bitmap?.let { bitmap ->
            Canvas(modifier = Modifier.matchParentSize().pointerInput(bitmap) {
                detectTapGestures(
                    onDoubleTap = {
                        scope.launch {
                            state.cropImage()?.let {
                                onCropped(it)
                                state.bitmap = null
                            }
                        }
                    }
                )
            }) {
                // Mask
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
                    drawRect(
                        color = Colors.Black, // option.maskColor
                        alpha = 0.5f, // option.maskAlpha
                    )
                    drawRect(
                        color = Colors.Transparent,
                        topLeft = state.frameRect.topLeft,
                        size = state.frameRect.size,
                        blendMode = BlendMode.SrcOut
                    )
                    canvas.restore()
                }
                // Frame
                drawRect(
                    color = Colors.White, // option.frameColor
                    alpha = 0.8f, // option.frameAlpha
                    topLeft = state.frameRect.topLeft,
                    size = state.frameRect.size,
                    style = Stroke(2.dp.toPx()) // option.frameWidth.toPx()
                )
                // Corner
                drawCorner(
                    rect = state.frameRect,
                    cornerLength = tolerance / 2,
                    frameColor = Colors.White, // option.frameColor
                    frameAlpha = 0.8f // option.frameAlpha
                )
                // Grid
                drawGrid(
                    rect = state.frameRect,
                    gridColor = Colors.White, // option.gridColor
                    gridAlpha = 0.6f // option.gridAlpha
                )
            }
        }

        LaunchedEffect(state.bitmap, state.aspectRatio, constraints) {
            state.bitmap?.let { bitmap ->
                val canvasSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

                val newSize = Size(canvasSize.width, canvasSize.width * bitmap.height / bitmap.width.toFloat())
                val imageSize = if (newSize.height > canvasSize.height)
                    (canvasSize.height / newSize.height).let { Size(newSize.width * it, newSize.height * it) }
                else newSize
                state.imageRect = Rect(
                    Offset((canvasSize.width - imageSize.width) / 2, (canvasSize.height - imageSize.height) / 2),
                    imageSize
                )
                val shortSide = min(state.imageRect.width, state.imageRect.height)
                val size = if (state.aspectRatio == 0f) Size(state.imageRect.width, state.imageRect.height)
                else {
                    val scale = shortSide / max(state.imageRect.width, state.imageRect.width / state.aspectRatio)
                    Size(state.imageRect.width * scale * 0.8f, state.imageRect.width * scale / state.aspectRatio * 0.8f)
                }
                state.frameRect = Rect(Offset((canvasSize.width - size.width) / 2, (canvasSize.height - size.height) / 2), size)
            }
        }
    }
}