package love.yinlin.compose.image.input

import love.yinlin.fs.File

internal expect suspend fun readPlatformImageFile(file: File, maxBytes: Int): ByteArray?