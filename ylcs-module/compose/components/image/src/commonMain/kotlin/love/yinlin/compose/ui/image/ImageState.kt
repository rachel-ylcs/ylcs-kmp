package love.yinlin.compose.ui.image

import androidx.compose.runtime.*
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

@Stable
class ImageState {
    var status by mutableStateOf(ImageLoadStatus.Empty)
        internal set

    internal var feedbackStatus: ImageLoadStatus? by mutableStateOf(null)
    internal var result: ImageResult? by mutableStateOf(null)

    var error: Throwable? by mutableStateOf(null)
        internal set

    val canRetry: Boolean get() = status == ImageLoadStatus.Error && error !is ImageDecodeException
    var progress by mutableStateOf(ImageProgress())
        internal set
    var size: IntSize? by mutableStateOf(null)
        internal set

    var isPlaying by mutableStateOf(true)

    val imageSize: IntSize? get() = result?.size
    val originalSize: IntSize? get() = result?.originalSize

    private var animated by mutableStateOf(false)
    val isAnimated: Boolean get() = animated

    val origin: ImageOrigin? get() = result?.origin
    private var zoomState: ImageZoomState? = null

    val zoom: ImageZoomState get() = zoomState ?: ImageZoomState().also {
        it.updateBounds(contentViewport, contentBounds)
        zoomState = it
    }

    private var geometry: ImageGeometry? by mutableStateOf(null)
    private var cachedGeometry: ImageGeometry? = null
    private var cachedResult: ImageResult? = null
    private var cachedViewport = IntSize.Zero
    private var cachedBounds = Rect.Zero
    internal val contentViewport: IntSize get() = size ?: IntSize.Zero

    private val contentBounds: Rect get() {
        val geometry = geometry ?: return Rect.Zero
        val result = activeResult(geometry.source, geometry.version, geometry.ready) ?: return Rect.Zero
        val viewport = contentViewport
        if (cachedGeometry !== geometry || cachedResult !== result || cachedViewport != viewport) {
            cachedGeometry = geometry
            cachedResult = result
            cachedViewport = viewport
            cachedBounds = geometry.bounds(result.size, viewport)
        }
        return cachedBounds
    }

    val contentRect: Rect get() {
        val bounds = contentBounds
        if (bounds.isEmpty) return Rect.Zero
        if (geometry?.zoom != true) return bounds
        val viewport = contentViewport
        val center = Offset(viewport.width / 2f, viewport.height / 2f)
        return Rect(center + (bounds.topLeft - center) * zoom.scale + zoom.translation,
            bounds.size * zoom.scale)
    }

    internal fun activeResult(source: ImageSource?, version: String?, ready: Boolean): ImageResult? =
        result.takeIf { ready && this.source == source && this.version == version }

    internal fun updateGeometry(geometry: ImageGeometry) {
        if (this.geometry != geometry) {
            this.geometry = geometry
            updateZoomBounds()
        }
    }

    internal fun updateSize(size: IntSize) {
        if (this.size != size) {
            this.size = size
            updateZoomBounds()
        }
    }

    private fun updateZoomBounds() {
        zoomState?.updateBounds(contentViewport, contentBounds)
    }

    internal var reloadKey by mutableIntStateOf(0)
        private set
    internal var source: ImageSource? = null
    internal var version: String? = null
    internal var retainedRequest: ImageRequest? = null
    internal var retainedGeneration = -1L
    internal var visible by mutableStateOf(false)

    fun retry() {
        retainedRequest = null
        reloadKey++
    }

    internal fun begin(source: ImageSource?, version: String?) {
        val changed = this.source != source || this.version != version
        if (changed) {
            result = null
            animated = false
            cachedResult = null
            cachedGeometry = null
            cachedBounds = Rect.Zero
            retainedRequest = null
            zoomState?.reset()
        }
        this.source = source
        this.version = version
        if (changed) updateZoomBounds()
        error = null
        progress = ImageProgress()
        status = if (source == null) ImageLoadStatus.Empty else ImageLoadStatus.Loading
        feedbackStatus = if (source == null) ImageLoadStatus.Empty else null
    }

    internal fun success(result: ImageResult) {
        this.result = result
        animated = result.isAnimated
        if (cachedResult !== result) cachedResult = null
        updateZoomBounds()
        error = null
        status = ImageLoadStatus.Success
        feedbackStatus = null
    }

    internal fun failure(error: Throwable) {
        this.error = error
        status = ImageLoadStatus.Error
        feedbackStatus = ImageLoadStatus.Error
    }
}

@Composable
fun rememberImageState(): ImageState = remember { ImageState() }