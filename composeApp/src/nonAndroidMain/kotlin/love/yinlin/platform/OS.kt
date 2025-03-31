package love.yinlin.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.createBitmap
import com.github.panpf.sketch.get
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use

actual val osStorageCacheSize: Long get() {
    val sketch = SingletonSketch.get()
    return sketch.downloadCache.size + sketch.resultCache.size
}

actual fun osStorageClearCache() {
    val sketch = SingletonSketch.get()
    sketch.downloadCache.clear()
    sketch.resultCache.clear()
}

actual fun osImageCrop(bitmap: ImageBitmap, startX: Int, startY: Int, width: Int, height: Int): ImageBitmap {
    val src = bitmap.asSkiaBitmap()
    val des = createBitmap(src.imageInfo.withWidthHeight(width = width, height = height))
    val canvas = Canvas(des)
    Image.makeFromBitmap(src).use {
        val left = startX.toFloat()
        val top = startY.toFloat()
        val right = startX + width.toFloat()
        val bottom = top + height.toFloat()
        canvas.drawImageRect(
            image = it,
            src = Rect(left, top, right, bottom),
            dst = Rect(0f, 0f, width.toFloat(), height.toFloat()),
            samplingMode = SamplingMode.CATMULL_ROM,
            paint = null,
            strict = true
        )
    }
    return des.asComposeImageBitmap()
}