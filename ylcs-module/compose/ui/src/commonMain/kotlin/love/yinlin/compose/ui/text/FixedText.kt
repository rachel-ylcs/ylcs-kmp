package love.yinlin.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalStyle

/**
 * 高性能 Text
 *
 * 自动跳过重组、布局阶段, 状态更新只发生在绘制阶段
 * 适用条件：固定布局，已知占位，单行，裁切
 */
@Composable
fun FixedText(
    placeholder: String,
    textProvider: () -> String?,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalStyle.current
) {
    val measurer = rememberTextMeasurer()
    val style = style.merge(color = color.takeOrElse { style.color.takeOrElse { LocalColor.current } })
    val result = remember(placeholder, style) { measurer.measure(text = placeholder, style = style, maxLines = 1) }

    Layout(
        modifier = modifier.drawBehind {
            textProvider()?.let { drawText(measurer, it, style = style, maxLines = 1) }
        }
    ) { _, _ ->
        layout(result.size.width, result.size.height) { }
    }
}