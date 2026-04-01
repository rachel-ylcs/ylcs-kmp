package love.yinlin.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.toSize
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalStyle

/**
 * 高性能 Text
 *
 * 自动跳过重组、布局阶段, 状态更新只发生在绘制阶段
 * 适用条件：固定大小，已知占位，单行，裁切
 */
@Composable
fun FastFixedText(
    placeholder: String,
    textProvider: () -> String?,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalStyle.current,
    measurer: TextMeasurer = rememberTextMeasurer()
) {
    val style = style.merge(color = color.takeOrElse { style.color.takeOrElse { LocalColor.current } })

    Layout(modifier = modifier.drawWithContent {
        textProvider()?.let { drawText(measurer, it, style = style, maxLines = 1) }
    }) { _, _ ->
        val result = measurer.measure(text = placeholder, style = style, maxLines = 1)
        layout(result.size.width, result.size.height) { }
    }
}

@Stable
data class FastTextResult(internal val delegate: TextLayoutResult) {
    val size: Size = delegate.size.toSize()
    val width: Float get() = size.width
    val height: Float get() = size.height
}

@Stable
data class FastTextLayoutScope(private val constraints: Constraints, private val scope: MeasureScope) {
    fun layout(measurer: TextMeasurer, text: String, style: TextStyle): MeasureResult {
        val result = measurer.measure(text = text, style = style, constraints = constraints, maxLines = 1)
        return scope.layout(constraints.maxWidth, result.size.height) { }
    }
}

@Stable
data class FastTextDrawScope(private val constraints: Constraints, @PublishedApi internal val scope: DrawScope) {
    fun measure(measurer: TextMeasurer, text: String, style: TextStyle): FastTextResult =
        FastTextResult(measurer.measure(text = text, style = style, constraints = constraints, maxLines = 1))

    inline fun draw(result: FastTextResult, drawBlock: DrawScope.(FastTextResult) -> Unit) = scope.withTransform({
        translate((scope.size.width - result.width).coerceAtLeast(0f) / 2, 0f)
        clipRect(0f, 0f, result.width, result.height)
    }) {
        drawBlock(result)
    }

    fun drawBackground(result: FastTextResult, color: Color) {
        scope.drawRect(color, size = result.size)
    }

    fun drawText(result: FastTextResult, color: Color) {
        result.delegate.multiParagraph.paint(scope.drawContext.canvas, color)
    }
}

/**
 * 高性能居中 Text
 *
 * 自动跳过重组和布局阶段, 状态更新只发生在绘制阶段
 * 适用条件：父容器宽度固定, 居中文本, 单行，裁切
 */
@Composable
fun FastCenterText(
    layoutAction: FastTextLayoutScope.() -> MeasureResult,
    drawAction: FastTextDrawScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(modifier = modifier.drawWithContent {
        FastTextDrawScope(Constraints(maxWidth = size.width.toInt()), this).drawAction()
    }) { _, constraints ->
        require(constraints.hasFixedWidth) { "FastCenterText should have fixed width" }
        FastTextLayoutScope(constraints, this).layoutAction()
    }
}