package love.yinlin.compose.image.decoder

import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.data.ImageResult
import love.yinlin.fs.File

internal actual val supportsPlatformImageFiles: Boolean = false

internal actual suspend fun decodePlatformImageFile(file: File, request: ImageDecodeOptions, maxBytes: Int): ImageResult? = null