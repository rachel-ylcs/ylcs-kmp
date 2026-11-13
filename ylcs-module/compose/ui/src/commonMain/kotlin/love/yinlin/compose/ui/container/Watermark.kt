package love.yinlin.compose.ui.container

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * 在 [content] 上绘制重复排列的 [mark] 水印，不改变内容的布局和触摸区域。
 *
 * [mark] 只组合并录制一次绘制指令，再以图层方式平铺；水印不创建覆盖在内容上的交互节点。
 *
 * @param modifier 容器修饰符
 * @param mark 单个水印标志的 Composable 内容
 * @param content 水印下方的内容
 * @param angle 逆时针旋转角度，单位为度
 * @param alpha 水印透明度对应的不透明系数，范围 0 ~ 1
 * @param space 相邻水印旋转后包围盒之间的最小间距，必须非负
 */
@Composable
fun Watermark(
    modifier: Modifier = Modifier,
    mark: @Composable () -> Unit,
    content: @Composable () -> Unit,
    angle: Float = 30f,
    alpha: Float = 0.12f,
    space: Dp = 48.dp,
) {
    require(angle.isFinite()) { "Watermark angle must be finite." }
    require(alpha in 0f .. 1f) { "Watermark alpha must be between 0 and 1." }
    require(space.value.isFinite() && space >= 0.dp) { "Watermark space must be finite and non-negative." }

    val markLayer = rememberGraphicsLayer()

    Layout(
        modifier = modifier.clipToBounds().drawWithContent {
            drawContent()

            val markWidth = markLayer.size.width.toFloat()
            val markHeight = markLayer.size.height.toFloat()
            if (alpha == 0f || markWidth <= 0f || markHeight <= 0f || size.width <= 0f || size.height <= 0f) return@drawWithContent

            val radians = (angle % 360f) * PI / 180.0
            val cosine = abs(cos(radians)).toFloat()
            val sine = abs(sin(radians)).toFloat()
            val rotatedWidth = markWidth * cosine + markHeight * sine
            val rotatedHeight = markWidth * sine + markHeight * cosine
            val columnStep = rotatedWidth + space.toPx()
            val rowStep = rotatedHeight + space.toPx()

            markLayer.alpha = alpha
            clipRect {
                val rows = ceil((size.height + rotatedHeight) / rowStep).toInt()
                for (row in 0 .. rows) {
                    val centerY = row * rowStep - rotatedHeight / 2f
                    val rowOffset = if (row % 2 == 0) 0f else columnStep / 2f
                    val columns = ceil((size.width + rotatedWidth + rowOffset) / columnStep).toInt()
                    for (column in 0 .. columns) {
                        val centerX = column * columnStep - rotatedWidth / 2f - rowOffset
                        withTransform({
                            translate(centerX - markWidth / 2f, centerY - markHeight / 2f)
                            rotate(-angle, pivot = Offset(markWidth / 2f, markHeight / 2f))
                        }) {
                            drawLayer(markLayer)
                        }
                    }
                }
            }
        },
        content = {
            Box(
                modifier = Modifier.clearAndSetSemantics { }.drawWithContent {
                    markLayer.record {
                        this@drawWithContent.drawContent()
                    }
                },
                content = { mark() },
            )
            Box(content = { content() })
        },
    ) { measurables, constraints ->
        val markPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val contentPlaceable = measurables[1].measure(constraints)
        layout(constraints.constrainWidth(contentPlaceable.width), constraints.constrainHeight(contentPlaceable.height)) {
            // The mark records into a graphics layer but paints nothing at its layout position.
            markPlaceable.place(0, 0)
            contentPlaceable.place(0, 0)
        }
    }
}