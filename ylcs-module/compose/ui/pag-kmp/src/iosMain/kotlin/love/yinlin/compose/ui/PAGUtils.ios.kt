@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreVideo.*

internal typealias PlatformPAG = cocoapods.libpag.PAG
internal typealias PlatformPAGComposition = cocoapods.libpag.PAGComposition
internal typealias PlatformPAGDiskCache = cocoapods.libpag.PAGDiskCache
internal typealias PlatformPAGDecoder = cocoapods.libpag.PAGDecoder
internal typealias PlatformPAGFile = cocoapods.libpag.PAGFile
internal typealias PlatformPAGFont = cocoapods.libpag.PAGFont
internal typealias PlatformPAGImage = cocoapods.libpag.PAGImage
internal typealias PlatformPAGImageLayer = cocoapods.libpag.PAGImageLayer
internal typealias PlatformPAGLayer = cocoapods.libpag.PAGLayer
internal typealias PlatformPAGMarker = cocoapods.libpag.PAGMarker
internal typealias PlatformPAGPlayer = cocoapods.libpag.PAGPlayer
internal typealias PlatformPAGShapeLayer = cocoapods.libpag.PAGShapeLayer
internal typealias PlatformPAGSolidLayer = cocoapods.libpag.PAGSolidLayer
internal typealias PlatformPAGSurface = cocoapods.libpag.PAGSurface
internal typealias PlatformPAGTextLayer = cocoapods.libpag.PAGTextLayer
internal typealias PlatformPAGVideoDecoder = cocoapods.libpag.PAGVideoDecoder
internal typealias PlatformPAGVideoRange = cocoapods.libpag.PAGVideoRange

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

internal val cocoapods.libpag.PAGLayerType.ordinal: Int get() = when (this) {
    cocoapods.libpag.PAGLayerType.PAGLayerTypeNull -> 1
    cocoapods.libpag.PAGLayerType.PAGLayerTypeSolid -> 2
    cocoapods.libpag.PAGLayerType.PAGLayerTypeText -> 3
    cocoapods.libpag.PAGLayerType.PAGLayerTypeShape -> 4
    cocoapods.libpag.PAGLayerType.PAGLayerTypeImage -> 5
    cocoapods.libpag.PAGLayerType.PAGLayerTypePreCompose -> 6
    else -> 0
}

internal val Int.asPAGLayerType: cocoapods.libpag.PAGLayerType get() = when (this) {
    1 -> cocoapods.libpag.PAGLayerType.PAGLayerTypeNull
    2 -> cocoapods.libpag.PAGLayerType.PAGLayerTypeSolid
    3 -> cocoapods.libpag.PAGLayerType.PAGLayerTypeText
    4 -> cocoapods.libpag.PAGLayerType.PAGLayerTypeShape
    5 -> cocoapods.libpag.PAGLayerType.PAGLayerTypeImage
    6 -> cocoapods.libpag.PAGLayerType.PAGLayerTypePreCompose
    else -> cocoapods.libpag.PAGLayerType.PAGLayerTypeUnknown
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