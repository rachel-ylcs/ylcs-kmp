package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.FontResource

@Stable
internal data class TextDrawCacheKey(
    val text: String,
    val height: Float,
    val fontWeight: FontWeight,
    val fontStyle: FontStyle,
    val letterSpacingRatio: Float,
    val fontResource: FontResource
)