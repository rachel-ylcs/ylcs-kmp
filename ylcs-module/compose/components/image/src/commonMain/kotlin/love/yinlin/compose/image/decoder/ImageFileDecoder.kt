package love.yinlin.compose.image.decoder

import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.data.ImageResult
import love.yinlin.fs.File

internal expect val supportsPlatformImageFiles: Boolean

internal expect suspend fun decodePlatformImageFile(file: File, request: ImageDecodeOptions, maxBytes: Int): ImageResult?