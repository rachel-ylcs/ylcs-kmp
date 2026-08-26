@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import platform.CoreVideo.*
import platform.posix.memcpy

import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAG
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGComposition
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGDecoder
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGDiskCache
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGFile
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGFont
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGImage
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGImageLayer
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGImageView
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGLayer
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGLayerType
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGMarker
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGPlayer
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGShapeLayer
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGSolidLayer
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGSurface
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGTextLayer
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGVideoDecoder
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGVideoRange
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGView
import swiftPMImport.love.yinlin.plugin.ylcs.module.plugin.pag.kmp.PAGViewListenerProtocol

internal typealias PlatformPAG = PAG
internal typealias PlatformPAGComposition = PAGComposition
internal typealias PlatformPAGDiskCache = PAGDiskCache
internal typealias PlatformPAGDecoder = PAGDecoder
internal typealias PlatformPAGFile = PAGFile
internal typealias PlatformPAGFont = PAGFont
internal typealias PlatformPAGImage = PAGImage
internal typealias PlatformPAGImageLayer = PAGImageLayer
internal typealias PlatformPAGImageView = PAGImageView
internal typealias PlatformPAGLayer = PAGLayer
internal typealias PlatformPAGMarker = PAGMarker
internal typealias PlatformPAGPlayer = PAGPlayer
internal typealias PlatformPAGShapeLayer = PAGShapeLayer
internal typealias PlatformPAGSolidLayer = PAGSolidLayer
internal typealias PlatformPAGSurface = PAGSurface
internal typealias PlatformPAGTextLayer = PAGTextLayer
internal typealias PlatformPAGVideoDecoder = PAGVideoDecoder
internal typealias PlatformPAGVideoRange = PAGVideoRange
internal typealias PlatformPAGView = PAGView
internal typealias PlatformPAGListener = PAGViewListenerProtocol

internal fun makePlatformPAGFont(fontFamily: String, fontStyle: String): PlatformPAGFont {
    val font = PlatformPAGFont()
    font.fontFamily = fontFamily
    font.fontStyle = fontStyle
    return font
}

internal fun makePlatformPAGMarker(startTime: Long, duration: Long, comment: String): PlatformPAGMarker {
    val marker = PlatformPAGMarker()
    marker.startTime = startTime
    marker.duration = duration
    marker.comment = comment
    return marker
}

internal fun makePlatformPAGVideoRange(startTime: Long, endTime: Long, playDuration: Long, reversed: Boolean): PlatformPAGVideoRange {
    val range = PlatformPAGVideoRange()
    range.startTime = startTime
    range.endTime = endTime
    range.playDuration = playDuration
    range.reversed = 0L
    return range
}

internal val Int.asPAGLayerType: PAGLayerType get() = when (this) {
    1 -> PAGLayerType.PAGLayerTypeNull
    2 -> PAGLayerType.PAGLayerTypeSolid
    3 -> PAGLayerType.PAGLayerTypeText
    4 -> PAGLayerType.PAGLayerTypeShape
    5 -> PAGLayerType.PAGLayerTypeImage
    6 -> PAGLayerType.PAGLayerTypePreCompose
    else -> PAGLayerType.PAGLayerTypeUnknown
}

internal val PAGColorType.asCVPixelFormat: UInt get() = when (this) {
    PAGColorType.ALPHA_8 -> kCVPixelFormatType_OneComponent8
    PAGColorType.RGBA_8888 -> kCVPixelFormatType_32RGBA
    PAGColorType.BGRA_8888 -> kCVPixelFormatType_32BGRA
    PAGColorType.RGB_565 -> kCVPixelFormatType_16LE565
    PAGColorType.GRAY_8 -> kCVPixelFormatType_OneComponent8
    PAGColorType.RGBA_F16 -> kCVPixelFormatType_64RGBAHalf
    PAGColorType.RGBA_1010102 -> kCVPixelFormatType_32RGBA
    PAGColorType.UNKNOWN -> kCVPixelFormatType_32ARGB
}

internal fun createImageFromPixelBuffer(pixelBuffer: CVPixelBufferRef): ImageBitmap? {
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