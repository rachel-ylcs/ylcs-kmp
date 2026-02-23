package love.yinlin.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalStyle

@Composable
fun RichText(
    text: RichString,
    modifier: Modifier = Modifier,
    renderer: RichRenderer = rememberRichRenderer(),
    style: TextStyle = LocalStyle.current,
    color: Color = Colors.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    fixLineHeight: Boolean = false,
) {
    val fontSize = LocalStyle.current.fontSize
    val result = remember(renderer, text) {
        renderer.render(fontSize, text)
    }
    Text(
        text = result.text,
        modifier = modifier,
        color = color,
        style = if (fixLineHeight) style.copy(lineHeight = TextUnit.Unspecified) else style, // what the fuck bug
        overflow = overflow,
        maxLines = maxLines,
        inlineContent = result.inlineContent
    )
}