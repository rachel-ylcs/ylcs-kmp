package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import love.yinlin.compose.extension.localComposition
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

@Composable
fun mainFont() : FontFamily {
    val font = LocalMainFontResource.current?.let { Font(it) }
    return remember(font) { font?.let { FontFamily(it) } ?: FontFamily.Default }
}

val LocalMainFontResource = localComposition<FontResource?>()

fun basicTextStyle(
    font: FontFamily,
    size: TextUnit,
    isBold: Boolean = false
): TextStyle = TextStyle(
    fontFamily = font,
    fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Light,
    fontSize = size,
    lineHeight = size * 1.5f,
    letterSpacing = size / 16f
)