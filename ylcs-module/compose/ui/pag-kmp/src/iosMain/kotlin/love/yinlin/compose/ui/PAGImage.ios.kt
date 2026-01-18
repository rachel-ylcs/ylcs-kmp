@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Matrix
import kotlinx.cinterop.*
import love.yinlin.compose.graphics.asCGAffineTransform
import love.yinlin.compose.graphics.asComposeMatrix
import platform.CoreVideo.*
import platform.darwin.ByteVar
import platform.posix.memcpy

actual class PAGImage(private val delegate: PlatformPAGImage) {
    actual val width: Int get() = delegate.width().toInt()
    actual val height: Int get() = delegate.height().toInt()
    actual var scaleMode: PAGScaleMode get() = PAGScaleMode.entries[delegate.scaleMode().toInt()]
        set(value) { delegate.setScaleMode(value.ordinal.toUInt()) }
    actual var matrix: Matrix get() = delegate.matrix().asComposeMatrix()
        set(value) { delegate.setMatrix(value.asCGAffineTransform()) }

    actual fun close() { }

    actual companion object {
        actual fun loadFromPath(path: String): PAGImage = PAGImage(PlatformPAGImage.FromPath(path)!!)

        actual fun loadFromBytes(bytes: ByteArray): PAGImage {
            val image = bytes.usePinned { PlatformPAGImage.FromBytes(it.addressOf(0), bytes.size.toULong())!! }
            return PAGImage(image)
        }

        actual fun loadFromPixels(pixels: IntArray, width: Int, height: Int, rowBytes: Long, colorType: PAGColorType, alphaType: PAGAlphaType): PAGImage {
            val pixelFormat = colorType.asCVPixelFormat
            val pixelBuffer = memScoped {
                val bufferPtr = alloc<CVPixelBufferRefVar>()
                CVPixelBufferCreate(null, width.toULong(), height.toULong(), pixelFormat, null, bufferPtr.ptr)
                bufferPtr.value
            }!!
            CVPixelBufferLockBaseAddress(pixelBuffer, 0UL)
            try {
                val baseAddress = CVPixelBufferGetBaseAddress(pixelBuffer)!!.reinterpret<ByteVar>()
                val bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer)
                pixels.usePinned { pinned ->
                    val srcPtr = pinned.addressOf(0)
                    val srcRowBytes = if (rowBytes > 0) rowBytes.toULong() else {
                        when (colorType) {
                            PAGColorType.RGBA_F16 -> (width * 8).toULong()
                            PAGColorType.RGB_565 -> (width * 2).toULong()
                            PAGColorType.ALPHA_8, PAGColorType.GRAY_8 -> (width * 1).toULong()
                            else -> (width * 4).toULong()
                        }
                    }
                    for (y in 0 until height) {
                        val destRow = baseAddress + (y.toULong() * bytesPerRow).toLong()
                        val srcRow = srcPtr.plus((y.toULong() * srcRowBytes).toLong())
                        memcpy(destRow, srcRow, srcRowBytes)
                    }
                }
            }
            finally {
                CVPixelBufferUnlockBaseAddress(pixelBuffer, 0UL)
            }
            return PAGImage(PlatformPAGImage.FromPixelBuffer(pixelBuffer))
        }
    }
}