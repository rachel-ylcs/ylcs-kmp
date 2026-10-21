package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable

@Stable
class ImageInputData(val bytes: ByteArray, val origin: ImageOrigin = ImageOrigin.Local)
