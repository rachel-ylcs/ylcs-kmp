package love.yinlin.compose.image.data

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import love.yinlin.extension.catchingNull

internal actual fun ImageBitmap.allocationBytes(): Long {
    val bitmap = asAndroidBitmap()
    check(!bitmap.isRecycled)

    val count = catchingNull { bitmap.allocationByteCount.toLong() }
    if (count != null) return count

    val colorSpace = when (bitmap.config) {
        Bitmap.Config.ALPHA_8 -> 1L
        Bitmap.Config.RGB_565, @Suppress("DEPRECATION") Bitmap.Config.ARGB_4444 -> 2L
        Bitmap.Config.RGBA_F16 -> 8L
        else -> 4L
    }
    return colorSpace * bitmap.width * bitmap.height
}