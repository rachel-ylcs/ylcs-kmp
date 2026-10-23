package love.yinlin.compose.image.decoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AnimatedImageDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.data.PlatformImageAnimation
import love.yinlin.compose.image.data.WebpInfo
import love.yinlin.extension.cleaning

internal class AndroidSystemWebpDecoder(
    private val bytes: ByteArray,
    private val info: WebpInfo,
    private val request: ImageDecodeOptions,
) : ImageDecoder {
    override val originalSize = info.size
    override val size = request.sampledSize(originalSize)
    override val durations = IntArray(info.frames.size) { info.frames[it].duration }
    override val repeatCount = info.repeatCount

    private var drawable: AnimatedImageDrawable? = AndroidAnimationFactory.decode(bytes, size, repeatCount)
    private var transfer: PlatformImageAnimation? = PlatformImageAnimation(requireNotNull(drawable))
    private var first: ImageBitmap? = null
    private var fallback: AndroidWebpDecoder? = null

    fun takePlatformAnimation(): PlatformImageAnimation? = transfer.also { transfer = null }

    override suspend fun frame(index: Int): ImageBitmap {
        require(index in durations.indices)
        currentCoroutineContext().ensureActive()

        if (index != 0) {
            val decoder = fallback ?: AndroidWebpDecoder(bytes, info, request).also { fallback = it }
            return decoder.frame(index)
        }
        first?.let { return it }

        val current = checkNotNull(drawable) { "Decoder is closed" }
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        var transferred = false

        return cleaning({
            current.setBounds(0, 0, size.width, size.height)
            current.draw(Canvas(bitmap))

            currentCoroutineContext().ensureActive()

            // 首个显示位置直接接管系统播放器；只有确实显示占位首帧时才准备其纹理
            bitmap.asImageBitmap().also {
                first = it
                transferred = true
            }
        }) {
            if (!transferred) bitmap.recycle()
        }
    }

    override fun close() {
        fallback?.close()
        fallback = null
        first = null
        drawable = null
        transfer = null
        // 未移交的播放器随解码器关闭释放；已移交的实例由活跃结果接管
    }
}