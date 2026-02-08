package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

@Composable
fun rememberFontFamily(
    resource: FontResource,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    variationSettings: FontVariation.Settings = FontVariation.Settings(weight, style)
): FontFamily {
    val font = Font(resource, weight, style, variationSettings)
    return remember(font) { FontFamily(font) }
}

@Composable
fun mainFont() : FontFamily = LocalMainFontResource.current?.let { rememberFontFamily(it) } ?: FontFamily.Default

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