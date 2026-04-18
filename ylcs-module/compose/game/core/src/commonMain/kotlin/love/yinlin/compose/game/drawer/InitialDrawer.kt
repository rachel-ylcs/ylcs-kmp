package love.yinlin.compose.game.drawer

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import love.yinlin.compose.game.asset.AssetProvider
import love.yinlin.compose.game.font.FontProvider
import org.jetbrains.compose.resources.FontResource

@Stable
open class InitialDrawer internal constructor(
    protected val density: Density,
    protected val fontFamilyResolver: FontFamily.Resolver,
    protected val fontProvider: FontProvider,
    val assetProvider: AssetProvider
) {
    companion object {
        val BaselineTextFontSize = 64.sp
    }

    fun measureText(
        text: String,
        font: FontResource? = null,
        fontWeight: FontWeight = FontWeight.Normal,
        fontStyle: FontStyle = FontStyle.Normal,
        letterSpacing: Float = 0f
    ): TextGraph {
        val intrinsics = ParagraphIntrinsics(
            text = text,
            style = TextStyle(
                fontSize = BaselineTextFontSize,
                fontWeight = fontWeight,
                fontFamily = fontProvider[font],
                fontStyle = fontStyle,
                letterSpacing = if (letterSpacing <= 0f) TextUnit.Unspecified else BaselineTextFontSize * letterSpacing
            ),
            annotations = emptyList(),
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            placeholders = emptyList()
        )
        val paragraph = Paragraph(
            paragraphIntrinsics = intrinsics,
            constraints = Constraints(maxWidth = intrinsics.maxIntrinsicWidth.toInt()),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        return TextGraph(paragraph)
    }
}