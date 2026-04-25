package love.yinlin.compose.game.drawer

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import love.yinlin.compose.game.asset.AssetProvider
import love.yinlin.compose.game.font.FontProvider
import org.jetbrains.compose.resources.FontResource

@Stable
open class InitialDrawer internal constructor(
    protected val fontFamilyResolver: FontFamily.Resolver,
    protected val fontProvider: FontProvider,
    val assetProvider: AssetProvider
) {
    companion object {
        private val DefaultDensity = Density(1f)
    }

    fun measureText(
        text: String,
        font: FontResource? = null,
        fontWeight: FontWeight = FontWeight.Normal,
        fontStyle: FontStyle = FontStyle.Normal,
        letterSpacing: Float = 0.015f,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode,
    ): TextGraph = TextGraph(
        text = text,
        font = font?.let { fontProvider[it] },
        density = DefaultDensity,
        fontFamilyResolver = fontFamilyResolver,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        letterSpacing = letterSpacing,
        shadow = shadow,
        textDecoration = textDecoration,
        blendMode = blendMode,
    )

    fun measureStrokeText(
        text: String,
        font: FontResource? = null,
        fontWeight: FontWeight = FontWeight.Normal,
        fontStyle: FontStyle = FontStyle.Normal,
        letterSpacing: Float = 0.015f,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode,
    ): StrokeTextGraph = StrokeTextGraph(
        text = text,
        font = font?.let { fontProvider[it] },
        density = DefaultDensity,
        fontFamilyResolver = fontFamilyResolver,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        letterSpacing = letterSpacing,
        shadow = shadow,
        textDecoration = textDecoration,
        blendMode = blendMode,
    )
}