package love.yinlin.compose

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

@Stable
data class TypographyTheme(
    val v1: TextStyle,
    val v2: TextStyle,
    val v3: TextStyle,
    val v4: TextStyle,
    val v5: TextStyle,
    val v6: TextStyle,
    val v7: TextStyle,
    val v8: TextStyle,
    val v9: TextStyle,
    val v10: TextStyle,
) {
    internal val default = v7

    companion object {
        internal val Number.style: TextStyle get() = this.toDouble().sp.let { TextStyle(
            fontSize = it,
            lineHeight = it * 1.3333f,
            letterSpacing = it * 0.0125f
        ) }

        val Default = TypographyTheme(
            v1 = 64.style,
            v2 = 48.style,
            v3 = 32.style,
            v4 = 24.style,
            v5 = 18.style,
            v6 = 16.style,
            v7 = 14.style,
            v8 = 12.style,
            v9 = 10.style,
            v10 = 8.style,
        )
    }

    internal fun updateFont(font: FontFamily): TypographyTheme = this.copy(
        v1 = v1.copy(fontFamily = font),
        v2 = v2.copy(fontFamily = font),
        v3 = v3.copy(fontFamily = font),
        v4 = v4.copy(fontFamily = font),
        v5 = v5.copy(fontFamily = font),
        v6 = v6.copy(fontFamily = font),
        v7 = v7.copy(fontFamily = font),
        v8 = v8.copy(fontFamily = font),
        v9 = v9.copy(fontFamily = font),
        v10 = v10.copy(fontFamily = font),
    )
}