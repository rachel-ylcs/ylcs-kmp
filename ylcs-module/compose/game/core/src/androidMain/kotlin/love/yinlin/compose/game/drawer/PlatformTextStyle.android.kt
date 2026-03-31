package love.yinlin.compose.game.drawer

import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformTextStyle

actual fun buildPlatformTextStyle(): PlatformTextStyle = PlatformTextStyle(
    spanStyle = null,
    paragraphStyle = PlatformParagraphStyle(includeFontPadding = false)
)