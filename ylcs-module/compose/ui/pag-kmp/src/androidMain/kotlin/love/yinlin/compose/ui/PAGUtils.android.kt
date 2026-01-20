package love.yinlin.compose.ui

import android.graphics.Bitmap
import android.os.Build

internal typealias PlatformPAG = org.libpag.PAG
internal typealias PlatformPAGComposition = org.libpag.PAGComposition
internal typealias PlatformPAGDiskCache = org.libpag.PAGDiskCache
internal typealias PlatformPAGDecoder = org.libpag.PAGDecoder
internal typealias PlatformPAGFile = org.libpag.PAGFile
internal typealias PlatformPAGFont = org.libpag.PAGFont
internal typealias PlatformPAGImage = org.libpag.PAGImage
internal typealias PlatformPAGImageLayer = org.libpag.PAGImageLayer
internal typealias PlatformPAGLayer = org.libpag.PAGLayer
internal typealias PlatformPAGMarker = org.libpag.PAGMarker
internal typealias PlatformPAGPlayer = org.libpag.PAGPlayer
internal typealias PlatformPAGShapeLayer = org.libpag.PAGShapeLayer
internal typealias PlatformPAGSolidLayer = org.libpag.PAGSolidLayer
internal typealias PlatformPAGSurface = org.libpag.PAGSurface
internal typealias PlatformPAGTextLayer = org.libpag.PAGTextLayer
internal typealias PlatformPAGVideoDecoder = org.libpag.VideoDecoder
internal typealias PlatformPAGVideoRange = org.libpag.PAGVideoRange

internal val PAGColorType.asAndroidBitmapConfig: Bitmap.Config get() = when (this) {
    PAGColorType.ALPHA_8 -> Bitmap.Config.ALPHA_8
    PAGColorType.RGB_565 -> Bitmap.Config.RGB_565
    PAGColorType.RGBA_F16 -> Bitmap.Config.RGBA_F16
    PAGColorType.RGBA_1010102 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Bitmap.Config.RGBA_1010102 else Bitmap.Config.ARGB_8888
    else -> Bitmap.Config.ARGB_8888
}