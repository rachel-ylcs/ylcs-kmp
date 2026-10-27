@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.image.input

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.IOException
import love.yinlin.fs.File
import platform.posix.FILE
import platform.posix.fopen

internal actual fun openNativeImageFile(path: String): CPointer<FILE> {
    require(File(path).isFileSync()) { "Image input is not a regular file" }
    return fopen(path, "rb") ?: throw IOException("Cannot open image file")
}
