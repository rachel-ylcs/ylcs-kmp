package love.yinlin.compose.ui

import android.graphics.Bitmap
import android.os.Build

typealias PlatformPAG = org.libpag.PAG
typealias PlatformPAGImage = org.libpag.PAGImage

val PAGColorType.asAndroidBitmapConfig: Bitmap.Config get() = when (this) {
    PAGColorType.ALPHA_8 -> Bitmap.Config.ALPHA_8
    PAGColorType.RGB_565 -> Bitmap.Config.RGB_565
    PAGColorType.RGBA_F16 -> Bitmap.Config.RGBA_F16
    PAGColorType.RGBA_1010102 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Bitmap.Config.RGBA_1010102 else Bitmap.Config.ARGB_8888
    else -> Bitmap.Config.ARGB_8888
}