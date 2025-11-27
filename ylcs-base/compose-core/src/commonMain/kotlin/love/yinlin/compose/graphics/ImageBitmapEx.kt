package love.yinlin.compose.graphics

import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.data.compose.ImageFormat
import love.yinlin.data.compose.ImageQuality

expect fun ByteArray.decodeImage(): ImageBitmap?
expect fun ImageBitmap.encodeImage(format: ImageFormat = ImageFormat.WEBP, quality: ImageQuality = ImageQuality.Full): ByteArray?