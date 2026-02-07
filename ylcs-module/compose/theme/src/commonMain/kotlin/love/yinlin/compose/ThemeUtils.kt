package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font

@Composable
fun mainFont() : FontFamily {
    val font = LocalMainFontResource.current?.let { Font(it) }
    return remember(font) { font?.let { FontFamily(it) } ?: FontFamily.Default }
}

val TextStyle.bold: TextStyle get() = this.copy(fontWeight = FontWeight.SemiBold)

fun TextStyle.scaleSize(ratio: Float, isBold: Boolean): TextStyle {
    val newStyle = with(TypographyTheme.Companion) { (fontSize.value * ratio).style }
    return this.copy(
        fontSize = newStyle.fontSize,
        fontWeight = if (isBold) FontWeight.SemiBold else this.fontWeight,
        lineHeight = newStyle.lineHeight,
        letterSpacing = newStyle.letterSpacing
    )
}