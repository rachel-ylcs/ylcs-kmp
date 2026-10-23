package love.yinlin.compose.image.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig

internal actual fun ImageBitmap.allocationBytes(): Long = (if (config == ImageBitmapConfig.F16) 8L else 4L) * width * height