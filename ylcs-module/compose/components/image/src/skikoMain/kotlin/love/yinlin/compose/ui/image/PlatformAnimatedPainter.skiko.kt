package love.yinlin.compose.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import love.yinlin.compose.image.data.ImageResult

@Composable
internal actual fun rememberPlatformAnimatedPainter(result: ImageResult, state: ImageState): Painter? = null