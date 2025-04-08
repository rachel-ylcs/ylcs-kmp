package love.yinlin.ui.component.image

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.size
import love.yinlin.common.Colors
import love.yinlin.extension.rememberDerivedState
import love.yinlin.extension.rememberState
import love.yinlin.extension.translate
import love.yinlin.platform.CropResult
import love.yinlin.platform.ImageQuality
import love.yinlin.ui.component.screen.DialogState
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

private sealed interface TouchRegion {
    enum class Vertex : TouchRegion {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
    data object Inside : TouchRegion
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
    rect.topLeft.translate(offsetFromVertex, offsetFromVertex).let { start ->
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
    rect.topRight.translate(-offsetFromVertex, offsetFromVertex).let { start ->
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
    rect.bottomLeft.translate(offsetFromVertex, -offsetFromVertex).let { start ->
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
    rect.bottomRight.translate(-offsetFromVertex, -offsetFromVertex).let { start ->
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

@Composable
fun CropImage(
    url: String?,
    aspectRatio: Float = 0f,
    modifier: Modifier = Modifier,
    onCrop: (CropResult) -> Unit
) {
    val tolerance = with(LocalDensity.current) { 24.dp.toPx() }
    val imageState = rememberWebImageState(quality = ImageQuality.High)
    val imageSize by rememberDerivedState { imageState.result?.image?.size }
    var touchRegion: TouchRegion? by rememberState { null }
    var frameRect: Rect by rememberState { Rect.Zero }
    var imageRect: Rect by rememberState { Rect.Zero }

    BoxWithConstraints(
        modifier = modifier.pointerInput(aspectRatio, imageSize) {
            if (imageSize != null) detectDragGestures(
                onDragStart = { position ->
                    touchRegion = frameRect.run { when {
                        Rect(topLeft, tolerance).contains(position) -> TouchRegion.Vertex.TOP_LEFT
                        Rect(topRight, tolerance).contains(position) -> TouchRegion.Vertex.TOP_RIGHT
                        Rect(bottomLeft, tolerance).contains(position) -> TouchRegion.Vertex.BOTTOM_LEFT
                        Rect(bottomRight, tolerance).contains(position) -> TouchRegion.Vertex.BOTTOM_RIGHT
                        contains(position) -> TouchRegion.Inside
                        else -> null
                    } }
                },
                onDragEnd = { touchRegion = null }
            ) { change, dragAmount ->
                touchRegion?.let {
                    frameRect = when (it) {
                        is TouchRegion.Vertex -> {
                            val minimumVertexDistance = tolerance * 2f
                            frameRect.run {
                                if (aspectRatio == 0f) {
                                    val newLeft = (left + dragAmount.x)
                                        .coerceAtLeast(imageRect.left)
                                        .coerceAtMost(right - minimumVertexDistance)
                                    val newTop = (top + dragAmount.y)
                                        .coerceAtLeast(imageRect.top)
                                        .coerceAtMost(bottom - minimumVertexDistance)
                                    val newRight = (right + dragAmount.x)
                                        .coerceAtMost(imageRect.right)
                                        .coerceAtLeast(left + minimumVertexDistance)
                                    val newBottom = (bottom + dragAmount.y)
                                        .coerceAtMost(imageRect.bottom)
                                        .coerceAtLeast(top + minimumVertexDistance)
                                    when (it) {
                                        TouchRegion.Vertex.TOP_LEFT -> Rect(newLeft, newTop, right, bottom)
                                        TouchRegion.Vertex.TOP_RIGHT -> Rect(left, newTop, newRight, bottom)
                                        TouchRegion.Vertex.BOTTOM_LEFT -> Rect(newLeft, top, right, newBottom)
                                        TouchRegion.Vertex.BOTTOM_RIGHT -> Rect(left, top, newRight, newBottom)
                                    }
                                }
                                else {
                                    val (a, b) = when (it) {
                                        TouchRegion.Vertex.TOP_LEFT, TouchRegion.Vertex.BOTTOM_RIGHT -> (bottom - top) / (right - left) to (right * top - left * bottom) / (right - left)
                                        else -> (top - bottom) / (right - left) to (right * bottom - left * top) / (right - left)
                                    }
                                    when (it) {
                                        TouchRegion.Vertex.TOP_LEFT -> {
                                            val rLeft = (left + dragAmount.x)
                                                .coerceAtLeast(imageRect.left)
                                                .coerceAtMost(right - minimumVertexDistance)
                                            val rTop = (a * rLeft + b)
                                                .coerceAtLeast(imageRect.top)
                                                .coerceAtMost(bottom - minimumVertexDistance)
                                            copy(left = (rTop - b) / a, top = rTop)
                                        }
                                        TouchRegion.Vertex.TOP_RIGHT -> {
                                            val rRight = (right + dragAmount.x)
                                                .coerceAtMost(imageRect.right)
                                                .coerceAtLeast(left + minimumVertexDistance)
                                            val rTop = (a * rRight + b)
                                                .coerceAtLeast(imageRect.top)
                                                .coerceAtMost(bottom - minimumVertexDistance)
                                            copy(right = (rTop - b) / a, top = rTop)
                                        }
                                        TouchRegion.Vertex.BOTTOM_LEFT -> {
                                            val rLeft = (left + dragAmount.x)
                                                .coerceAtLeast(imageRect.left)
                                                .coerceAtMost(right - minimumVertexDistance)
                                            val rBottom = (a * rLeft + b)
                                                .coerceAtMost(imageRect.bottom)
                                                .coerceAtLeast(top + minimumVertexDistance)
                                            copy(left = (rBottom - b) / a, bottom = rBottom)
                                        }
                                        TouchRegion.Vertex.BOTTOM_RIGHT -> {
                                            val rRight = (right + dragAmount.x)
                                                .coerceAtMost(imageRect.right)
                                                .coerceAtLeast(left + minimumVertexDistance)
                                            val rBottom = (a * rRight + b)
                                                .coerceAtMost(imageRect.bottom)
                                                .coerceAtLeast(top + minimumVertexDistance)
                                            copy(right = (rBottom - b) / a, bottom = rBottom)
                                        }
                                    }
                                }
                            }
                        }
                        TouchRegion.Inside -> {
                            var newRect = frameRect.translate(dragAmount)
                            if (newRect.left < imageRect.left) newRect = newRect.translate(imageRect.left - newRect.left, 0f)
                            if (newRect.right > imageRect.right) newRect = newRect.translate(imageRect.right - newRect.right, 0f)
                            if (newRect.top < imageRect.top) newRect = newRect.translate(0f, imageRect.top - newRect.top)
                            if (newRect.bottom > imageRect.bottom) newRect = newRect.translate(0f, imageRect.bottom - newRect.bottom)
                            newRect
                        }
                    }
                    change.consume()
                }
            }
        }
    ) {
        // 当图像、比例、容器变化时调整图像和裁剪框的大小
        LaunchedEffect(aspectRatio, imageSize, constraints) {
            imageSize?.let { actualSize ->
                val canvasSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                val newSize = Size(canvasSize.width, canvasSize.width * actualSize.height / actualSize.width.toFloat())
                val imageSize = if (newSize.height > canvasSize.height) {
                    (canvasSize.height / newSize.height).let { Size(newSize.width * it, newSize.height * it) }
                } else newSize
                imageRect = Rect(
                    offset = Offset(
                        x = (canvasSize.width - imageSize.width) / 2,
                        y = (canvasSize.height - imageSize.height) / 2
                    ),
                    size = imageSize
                )
                val frameSize = if (aspectRatio == 0f) {
                    Size(imageRect.width * 0.8f, imageRect.height * 0.8f)
                } else {
                    val scale = min(imageRect.width, imageRect.height) / max(imageRect.width, imageRect.width / aspectRatio)
                    Size(imageRect.width * scale * 0.8f, imageRect.width * scale / aspectRatio * 0.8f)
                }
                frameRect = Rect(
                    offset = Offset(
                        x = (canvasSize.width - frameSize.width) / 2,
                        y = (canvasSize.height - frameSize.height) / 2
                    ),
                    size = frameSize
                )
            }
        }

        // Image
        Box(modifier = Modifier.matchParentSize().background(Colors.Black)) {
            if (url != null) {
                WebImage(
                    uri = url,
                    state = imageState,
                    quality = ImageQuality.High,
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        // Overlay
        imageSize?.let { actualSize ->
            Canvas(modifier = Modifier.matchParentSize().pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    // 在裁剪框内双击触发
                    if (frameRect.contains(it)) {
                        onCrop(CropResult(
                            xPercent = (frameRect.left - imageRect.left) / imageRect.width,
                            yPercent = (frameRect.top - imageRect.top) / imageRect.height,
                            widthPercent = frameRect.width / imageRect.width,
                            heightPercent = frameRect.height / imageRect.height
                        ))
                    }
                })
            }) {
                // Mask
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
                    drawRect(color = Colors.Black, alpha = 0.5f)
                    drawRect(
                        color = Colors.Transparent,
                        topLeft = frameRect.topLeft,
                        size = frameRect.size,
                        blendMode = BlendMode.SrcOut
                    )
                    canvas.restore()
                }
                // Frame
                drawRect(
                    color = Colors.White,
                    alpha = 0.8f,
                    topLeft = frameRect.topLeft,
                    size = frameRect.size,
                    style = Stroke(2.dp.toPx())
                )
                // Corner
                drawCorner(
                    rect = frameRect,
                    cornerLength = tolerance / 2,
                    frameColor = Colors.White,
                    frameAlpha = 0.8f
                )
                // Grid
                drawGrid(
                    rect = frameRect,
                    gridColor = Colors.White,
                    gridAlpha = 0.6f
                )
            }
        }
    }
}

class DialogCrop : DialogState<CropResult>() {
    private var url: String? by mutableStateOf(null)
    private var aspectRatio: Float by mutableStateOf(0f)

    suspend fun open(url: String, aspectRatio: Float = 0f): CropResult? {
        this.url = url
        this.aspectRatio = aspectRatio
        return awaitResult()
    }

    @Composable
    override fun dialogContent() {
        BaseDialog {
            CropImage(
                url = url,
                aspectRatio = aspectRatio,
                onCrop = { continuation?.resume(it) },
                modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth().aspectRatio(1f)
            )
        }
    }
}