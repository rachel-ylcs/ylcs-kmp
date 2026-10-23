package love.yinlin.compose.image.decoder

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.ensureActive
import love.yinlin.extension.cleaning
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.SamplingMode
import kotlin.coroutines.CoroutineContext

internal fun Bitmap.copyImageBitmap(size: IntSize, context: CoroutineContext, sampling: SamplingMode = SamplingMode.LINEAR): ImageBitmap {
    val output = Bitmap()
    var transferred = false

    return cleaning({
        val scaled = width != size.width || height != size.height
        check(output.allocPixels(if (scaled) imageInfo.withWidthHeight(size.width, size.height) else imageInfo))

        val source = requireNotNull(peekPixels())

        cleaning({
            val destination = requireNotNull(output.peekPixels())

            cleaning({
                check(if (!scaled) source.readPixels(destination) else source.scalePixels(destination, sampling))
            }, destination::close)
        }, source::close)

        context.ensureActive()

        output.setImmutable()
        output.asComposeImageBitmap().also { transferred = true }
    }) {
        if (!transferred) output.close()
    }
}