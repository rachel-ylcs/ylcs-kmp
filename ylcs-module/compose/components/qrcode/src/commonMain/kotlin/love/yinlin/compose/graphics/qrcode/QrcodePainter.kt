package love.yinlin.compose.graphics.qrcode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 生成用于 Image 的二维码 Painter，默认使用直角图案，预留四个码点宽的静区。
 *
 * 数据码点和定位点使用 [color] 到 [secondaryColor] 的对角渐变，定位框使用 [color]。
 * [background] 默认为透明，同时用于静区和 Logo 留白；透明时由调用方提供合适的外层背景。
 * 中央 [logo] 保留宽高比，长边为码区边长的 25%，四周留半个码点的间隙。
 * [ecLevel] 默认为 H；带 Logo 时建议保留该等级。
 *
 * 只在 [text] 或容错等级变化时重新编码。文本超出该等级的最大容量时抛出 [IllegalArgumentException]。
 */
@Composable
fun rememberQrcodePainter(
    text: String,
    logo: Painter? = null,
    shape: QrcodeShape = QrcodeShape.Square,
    color: Color = Color.Black,
    secondaryColor: Color = color,
    ecLevel: QrcodeECLevel = QrcodeECLevel.H,
    background: Color = Color.Transparent,
): Painter {
    val matrix = remember(text, ecLevel) { QrcodeEncoder.encode(text, ecLevel) }
    return remember(matrix, logo, shape, color, secondaryColor, background) {
        QrcodePainter(matrix, logo, shape, color, secondaryColor, background)
    }
}

@Stable
internal class QrcodePainter(
    private val matrix: QrcodeMatrix,
    private val logo: Painter?,
    private val shape: QrcodeShape,
    color: Color,
    secondaryColor: Color,
    private val background: Color,
) : Painter() {
    private val side = matrix.size.toFloat()
    private val color = color.qrcodeColor(background)
    override val intrinsicSize: Size = Size((side + 8f) * 10f, (side + 8f) * 10f)

    private val logoSize: Size = if (logo == null) Size.Zero else {
        val intrinsic = logo.intrinsicSize
        val ratio = (intrinsic.width / intrinsic.height).takeIf { it.isFinite() && it > 0f } ?: 1f
        if (ratio > 1f) Size(side * 0.25f, side * 0.25f / ratio)
        else Size(side * 0.25f * ratio, side * 0.25f)
    }
    private val dataPath = createDataPath()
    private val framePath = Path().apply {
        fillType = PathFillType.EvenOdd
        addFrame(0f, 0f)
        addFrame(side - 7f, 0f)
        addFrame(0f, side - 7f)
    }
    private val brush = Brush.linearGradient(
        0f to this.color,
        1f to secondaryColor.qrcodeColor(background),
        end = Offset(side, side),
    )

    private val cacheScope = CanvasDrawScope()
    private var cachedImage: ImageBitmap? = null
    private var cachedSize: Size = Size.Unspecified
    private var cachedDensity = Float.NaN
    private var cachedFontScale = Float.NaN
    private var cachedDirection: LayoutDirection? = null
    private var alpha = 1f
    private var colorFilter: ColorFilter? = null

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        if (size.minDimension <= 0f || !size.width.isFinite() || !size.height.isFinite()) return

        var image = cachedImage
        if (image == null || cachedSize != size || cachedDensity != density || cachedFontScale != fontScale || cachedDirection != layoutDirection) {
            val width = ceil(size.width).toInt()
            val height = ceil(size.height).toInt()
            val target = image?.takeIf { it.width == width && it.height == height } ?: ImageBitmap(width, height)
            val unit = size.minDimension / (side + 8f)
            val pixelSize = if (unit >= 1f) floor(unit) else unit
            val left = floor((size.width - side * pixelSize) / 2f)
            val top = floor((size.height - side * pixelSize) / 2f)
            cacheScope.draw(this, layoutDirection, Canvas(target), Size(width.toFloat(), height.toFloat())) {
                drawRect(background, blendMode = BlendMode.Src)
                translate(left, top) {
                    scale(pixelSize, pixelSize, Offset.Zero) {
                        drawPath(dataPath, brush)
                        drawPath(framePath, color)
                        logo?.run {
                            val x = (side - logoSize.width) / 2f
                            val y = (side - logoSize.height) / 2f
                            drawRoundRect(
                                color = background,
                                topLeft = Offset(x - 0.5f, y - 0.5f),
                                size = Size(logoSize.width + 1f, logoSize.height + 1f),
                                cornerRadius = CornerRadius(0.5f, 0.5f),
                                blendMode = BlendMode.Src,
                            )
                            translate(x, y) { draw(logoSize) }
                        }
                    }
                }
            }
            target.prepareToDraw()
            cachedImage = target
            cachedSize = size
            cachedDensity = density
            cachedFontScale = fontScale
            cachedDirection = layoutDirection
            image = target
        }
        drawImage(image, alpha = alpha, colorFilter = colorFilter)
    }

    private fun createDataPath(): Path = Path().apply {
        fillType = PathFillType.EvenOdd
        for (x in 0 ..< matrix.size) {
            for (y in 0 ..< matrix.size) {
                val insideFinder = x <= 7 && y <= 7 || x <= 7 && y >= matrix.size - 8 ||
                    x >= matrix.size - 8 && y <= 7
                if (!insideFinder && matrix[x, y]) addModule(x, y)
            }
        }
        addBall(2f, 2f)
        addBall(side - 5f, 2f)
        addBall(2f, side - 5f)
    }

    private fun Path.addModule(x: Int, y: Int) {
        val rect = Rect(x.toFloat(), y.toFloat(), x + 1f, y + 1f)
        if (shape == QrcodeShape.Square || matrix.isFunction(x, y)) addRect(rect)
        else {
            fun dark(x: Int, y: Int): Boolean = x in 0 ..< matrix.size && y in 0 ..< matrix.size && matrix[x, y]

            val top = dark(x, y - 1)
            val bottom = dark(x, y + 1)
            val left = dark(x - 1, y)
            val right = dark(x + 1, y)
            val corner = CornerRadius(0.5f, 0.5f)

            addRoundRect(RoundRect(
                rect = rect,
                topLeft = if (!top && !left) corner else CornerRadius.Zero,
                topRight = if (!top && !right) corner else CornerRadius.Zero,
                bottomRight = if (!bottom && !right) corner else CornerRadius.Zero,
                bottomLeft = if (!bottom && !left) corner else CornerRadius.Zero,
            ))
        }
    }

    private fun Path.addFrame(x: Float, y: Float) {
        val outer = Rect(x, y, x + 7f, y + 7f)
        val inner = Rect(x + 1f, y + 1f, x + 6f, y + 6f)
        if (shape == QrcodeShape.Square) {
            addRect(outer)
            addRect(inner)
        }
        else {
            addRoundRect(RoundRect(outer, CornerRadius(0.4f, 0.4f)))
            addRoundRect(RoundRect(inner, CornerRadius(0.75f, 0.75f)))
        }
    }

    private fun Path.addBall(x: Float, y: Float) {
        val rect = Rect(x, y, x + 3f, y + 3f)
        if (shape == QrcodeShape.Square) addRect(rect)
        else addRoundRect(RoundRect(rect, CornerRadius(0.75f, 0.75f)))
    }
}

private fun Color.qrcodeColor(background: Color): Color {
    if (background.alpha < 1f) return this

    val opaque = compositeOver(background)
    if (background.luminance() < 0.5f || opaque.luminance() <= 0.2f) return opaque

    var low = 0f
    var high = 1f
    repeat(8) {
        val middle = (low + high) / 2f
        if (lerp(Color.Black, opaque, middle).luminance() <= 0.2f) low = middle
        else high = middle
    }
    return lerp(Color.Black, opaque, low)
}
