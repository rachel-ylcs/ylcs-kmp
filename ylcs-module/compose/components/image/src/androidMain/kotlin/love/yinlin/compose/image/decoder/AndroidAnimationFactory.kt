package love.yinlin.compose.image.decoder

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import love.yinlin.compose.image.data.ImageAnimationData
import love.yinlin.coroutines.Coroutines
import java.nio.ByteBuffer

internal object AndroidAnimationFactory {
    private val decodes = Semaphore(4)

    suspend fun create(animation: ImageAnimationData): AnimatedImageDrawable = decodes.withPermit {
        Coroutines.cpu {
            currentCoroutineContext().ensureActive()
            decode(animation.bytes, animation.size, animation.repeatCount)
        }
    }

    fun decode(bytes: ByteArray, size: IntSize, repeatCount: Int): AnimatedImageDrawable {
        val buffer = if (Build.VERSION.SDK_INT >= 30) {
            ByteBuffer.wrap(bytes)
        }
        else ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            (this as java.nio.Buffer).rewind()
        }

        val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(buffer)) { decoder, _, _ ->
            decoder.setTargetSize(size.width, size.height)
        }
        check(drawable is AnimatedImageDrawable)

        drawable.repeatCount = repeatCount
        return drawable
    }
}