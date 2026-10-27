@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.image.input

import kotlinx.cinterop.*
import kotlinx.io.IOException
import platform.posix.FILE
import platform.posix._wfopen
import platform.windows.FILE_ATTRIBUTE_DIRECTORY
import platform.windows.GetFileAttributesW
import platform.windows.INVALID_FILE_ATTRIBUTES

internal actual fun openNativeImageFile(path: String): CPointer<FILE> = memScoped {
    val attributes = GetFileAttributesW(path)
    require(attributes != INVALID_FILE_ATTRIBUTES && attributes and FILE_ATTRIBUTE_DIRECTORY.toUInt() == 0U) { "Image input is not a regular file" }
    _wfopen(path.wcstr.ptr, "rb".wcstr.ptr) ?: throw IOException("Cannot open image file")
}
