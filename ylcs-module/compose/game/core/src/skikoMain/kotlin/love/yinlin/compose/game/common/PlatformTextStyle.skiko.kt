package love.yinlin.compose.game.common

import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformTextStyle

actual fun buildPlatformTextStyle(): PlatformTextStyle = PlatformTextStyle(
    spanStyle = null,
    paragraphStyle = PlatformParagraphStyle.Default
)