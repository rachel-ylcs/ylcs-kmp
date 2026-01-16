@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import cocoapods.libpag.*
import kotlinx.cinterop.*
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import platform.CoreVideo.*
import platform.posix.memcpy

@Suppress("FunctionName")
fun PAGFile.Companion.Load(data: ByteArray) = data.usePinned {
    PAGFile.Load(it.addressOf(0), data.size.toULong())
}

fun createImageFromPixelBuffer(pixelBuffer: CVPixelBufferRef): ImageBitmap? {
    CVPixelBufferLockBaseAddress(pixelBuffer, kCVPixelBufferLock_ReadOnly)

    val width = CVPixelBufferGetWidth(pixelBuffer).toInt()
    val height = CVPixelBufferGetHeight(pixelBuffer).toInt()
    val bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer).toInt()
    val baseAddress = CVPixelBufferGetBaseAddress(pixelBuffer)

    if (baseAddress == null) {
        CVPixelBufferUnlockBaseAddress(pixelBuffer, kCVPixelBufferLock_ReadOnly)
        return null
    }

    val pixels = ByteArray(height * bytesPerRow)
    memcpy(pixels.refTo(0), baseAddress, (height * bytesPerRow).toULong())

    CVPixelBufferUnlockBaseAddress(pixelBuffer, kCVPixelBufferLock_ReadOnly)

    return Image.makeRaster(
        imageInfo = ImageInfo.makeN32Premul(width, height),
        bytes = pixels,
        rowBytes = bytesPerRow
    ).toComposeImageBitmap()
}