@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreVideo.*

internal typealias PlatformPAG = cocoapods.libpag.PAG
internal typealias PlatformPAGDiskCache = cocoapods.libpag.PAGDiskCache
internal typealias PlatformPAGFont = cocoapods.libpag.PAGFont
internal typealias PlatformPAGImage = cocoapods.libpag.PAGImage
internal typealias PlatformPAGLayer = cocoapods.libpag.PAGLayer
internal typealias PlatformPAGMarker = cocoapods.libpag.PAGMarker
internal typealias PlatformPAGShapeLayer = cocoapods.libpag.PAGShapeLayer
internal typealias PlatformPAGSolidLayer = cocoapods.libpag.PAGSolidLayer
internal typealias PlatformPAGVideoRange = cocoapods.libpag.PAGVideoRange

internal val cocoapods.libpag.PAGLayerType.ordinal: Int get() = when (this) {
    PAGLayerTypeNull -> 1
    PAGLayerTypeSolid -> 2
    PAGLayerTypeText -> 3
    PAGLayerTypeShape -> 4
    PAGLayerTypeImage -> 5
    PAGLayerTypePreCompose -> 6
    else -> 0
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