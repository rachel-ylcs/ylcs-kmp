package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

@Composable
fun mainFont() : Font = Font(LocalMainFontResource.current)

val LocalMainFontResource = localComposition<FontResource>()

fun basicTextStyle(
    font: Font,
    size: TextUnit,
    isBold: Boolean = false
): TextStyle = TextStyle(
    fontFamily = FontFamily(font),
    fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Light,
    fontSize = size,
    lineHeight = size * 1.5f,
    letterSpacing = size / 16f
)