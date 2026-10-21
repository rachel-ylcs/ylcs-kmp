package love.yinlin.compose.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.image.data.ImageLoadStatus
import love.yinlin.compose.image.data.ImageOrigin
import love.yinlin.compose.image.data.ImageProgress
import love.yinlin.compose.image.data.ImageRequest
import love.yinlin.compose.image.data.ImageResult
import love.yinlin.compose.image.decoder.ImageDecodeException
import love.yinlin.compose.image.input.ImageSource

/** 单个显示位置的状态和 UI 缓存；在 Compose 主线程观察/操作，不由不同位置共用。 */
@Stable
class ImageState {
    var status by mutableStateOf(ImageLoadStatus.Empty)
        internal set
    internal var result: ImageResult? by mutableStateOf(null)
    var error: Throwable? by mutableStateOf(null)
        internal set
    /** 读取失败可重试；数据无法解码时默认占位不提供重试入口。 */
    val canRetry: Boolean get() = status == ImageLoadStatus.Error && error !is ImageDecodeException
    var progress by mutableStateOf(ImageProgress())
        internal set
    var size: IntSize? by mutableStateOf(null)
        internal set
    var isPlaying by mutableStateOf(true)
    val imageSize: IntSize? get() = result?.size
    val originalSize: IntSize? get() = result?.originalSize
    val isAnimated: Boolean get() = result?.isAnimated == true
    val origin: ImageOrigin? get() = result?.origin
    val zoom = ImageZoomState()

    private var contentBounds by mutableStateOf(Rect.Zero)
    private var zoomEnabled by mutableStateOf(false)
    internal var contentViewport by mutableStateOf(IntSize.Zero)
        private set

    /** 图片在组件本地坐标中的实际绘制区域，包含 ContentScale、对齐和缩放平移，可能超出组件边界。 */
    val contentRect: Rect get() {
        if (result == null || contentBounds.isEmpty) return Rect.Zero
        if (!zoomEnabled) return contentBounds
        val viewport = contentViewport
        val center = Offset(viewport.width / 2f, viewport.height / 2f)
        return Rect(center + (contentBounds.topLeft - center) * zoom.scale + zoom.translation,
            contentBounds.size * zoom.scale)
    }

    internal fun updateContentBounds(bounds: Rect, viewport: IntSize, zoom: Boolean) {
        contentBounds = bounds
        contentViewport = viewport
        zoomEnabled = zoom
    }

    internal var reloadKey by mutableIntStateOf(0)
        private set
    internal var source: ImageSource? = null
    internal var version = ""
    internal var retainedRequest: ImageRequest? = null
    internal var retainedGeneration = -1L
    internal var visible by mutableStateOf(false)

    /** 使用相同输入源和版本重试加载，已有缓存仍可复用；刷新内容请改变图片的 vararg key。 */
    fun retry() {
        retainedRequest = null
        reloadKey++
    }

    internal fun begin(source: ImageSource?, version: String) {
        if (this.source != source || this.version != version) {
            result = null
            retainedRequest = null
            zoom.reset()
        }
        this.source = source
        this.version = version
        error = null
        progress = ImageProgress()
        status = if (source == null) ImageLoadStatus.Empty else ImageLoadStatus.Loading
    }

    internal fun success(result: ImageResult) {
        this.result = result
        error = null
        status = ImageLoadStatus.Success
    }

    internal fun failure(error: Throwable) {
        this.error = error
        status = ImageLoadStatus.Error
    }
}

@Composable
fun rememberImageState(): ImageState = remember { ImageState() }
