package love.yinlin.media

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import love.yinlin.compose.ui.media.VideoActionBar
import love.yinlin.compose.ui.media.VideoController
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.mainContext
import love.yinlin.coroutines.ioContext
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import org.jetbrains.skia.*

@Stable
internal class PcVideoController(topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoController(topBar, bottomBar) {
    private val backend = AudioBackend(video = true)
    private val scope = CoroutineScope(SupervisorJob() + mainContext)
    private val playback = MutableStateFlow(false)
    private val frameRequests = Channel<Unit>(Channel.CONFLATED)
    private val paint = Paint().apply { isAntiAlias = true }
    private var desiredPlaying = true
    private var pendingSeek: Long? = null
    private var activeSurfaces = 0
    private var frameVersion by mutableLongStateOf(0L)
    private var image: Image? = null
    private var imageSerial = -1L
    private var imageContext: DirectContext? = null
    private val initialized = scope.async {
        error = catchingError { backend.init() }
    }

    init {
        if (!VideoNative.requiresRenderingContext) {
            scope.launch(ioContext) {
                while (frameRequests.receiveCatching().isSuccess) {
                    val revision = backend.revision
                    var next: Image? = null
                    var accepted = false

                    cleaning({
                        val failure = catchingError {
                            next = backend.withHandle(null) { handle ->
                                VideoNative.info(handle, revision)?.let { VideoNative.snapshot(handle, revision, null, it) }
                            }
                        }

                        Coroutines.main {
                            if (!backend.isClosed && revision == backend.revision) {
                                if (failure != null) {
                                    error = failure
                                    desiredPlaying = false
                                    backend.pause()
                                    updatePlayback(false)
                                }
                                else if (next != null) {
                                    image?.close()
                                    image = next
                                    accepted = true
                                    ++frameVersion
                                }
                            }
                        }
                    }) {
                        if (!accepted) next?.close()
                    }
                }
            }
        }

        scope.launch {
            playback.collectLatest { playing ->
                if (playing) {
                    while (this@launch.isActive && !backend.isClosed) {
                        position = backend.query().position
                        delay(DefaultUpdateFrequency)
                    }
                }
            }
        }

        scope.launch {
            for ((val revision, val kind, val value, val message) in backend.events) {
                yield()
                if (backend.isClosed || revision != backend.revision) continue
                when (kind) {
                    AudioEvent.DURATION -> duration = value.coerceAtLeast(0L)
                    AudioEvent.PLAYBACK_STATE -> updatePlayback(backend.query().isPlaying)
                    AudioEvent.FRAME -> {
                        if (VideoNative.requiresRenderingContext) ++frameVersion
                        else if (activeSurfaces > 0) frameRequests.trySend(Unit)
                    }
                    AudioEvent.ERROR -> {
                        desiredPlaying = false
                        updatePlayback(false)
                        error = IllegalStateException(message ?: "Video playback failed: $value")
                    }
                    AudioEvent.ENDED -> {
                        updatePlayback(false)
                        if (url != null && desiredPlaying) {
                            backend.seek(0L)
                            backend.play()
                        }
                    }
                }
            }
        }
    }

    private fun updatePlayback(playing: Boolean) {
        isPlaying = playing
        playback.value = playing
    }

    override fun load(path: String) {
        scope.launch {
            if (backend.isClosed || url == path && error == null) return@launch
            val invalid = catchingError {
                require(path.isNotEmpty() && '\u0000' !in path) { "Invalid video path" }
            }
            if (invalid != null) {
                error = invalid
                return@launch
            }
            val request = backend.nextRequest()

            url = path
            error = null
            position = 0L
            duration = 0L
            updatePlayback(false)
            desiredPlaying = true
            pendingSeek = null
            clearFrame()
            initialized.await()
            if (!backend.isInit) return@launch

            val failure = catchingError {
                check(backend.load(path, request)) { "Cannot load video: ${backend.query().error}" }
            }
            backend.checkCurrent(request)

            if (failure != null) error = failure
            else {
                pendingSeek?.let { backend.seek(it, request) }
                pendingSeek = null
                if (desiredPlaying) backend.play(request)
            }
        }
    }

    override fun play() {
        scope.launch {
            desiredPlaying = true
            backend.play()
        }
    }

    override fun pause() {
        scope.launch {
            desiredPlaying = false
            backend.pause()
            updatePlayback(false)
        }
    }

    override fun stop() {
        scope.launch {
            desiredPlaying = false
            pendingSeek = null
            url = null
            updatePlayback(false)
            position = 0L
            duration = 0L
            clearFrame()
            backend.stopAndWait()
        }
    }

    override fun seek(position: Long) {
        scope.launch {
            if (url == null) return@launch
            val target = position.coerceAtLeast(0L)
            pendingSeek = if (backend.seek(target)) null else target
            desiredPlaying = true
            backend.play()
            this@PcVideoController.position = backend.query().position
        }
    }

    private fun clearFrame() {
        image?.close()
        image = null
        imageSerial = -1L
        imageContext = null
        ++frameVersion
    }

    override fun releaseController() {
        if (backend.isClosed) return
        backend.release()
        frameRequests.close()
        scope.cancel()
        CoroutineScope(mainContext).launch {
            isPlaying = false
            clearFrame()
            paint.close()
        }
    }

    @Composable
    override fun SurfaceContent(modifier: Modifier) {
        DisposableEffect(Unit) {
            ++activeSurfaces
            if (!VideoNative.requiresRenderingContext) frameRequests.trySend(Unit)
            onDispose { --activeSurfaces }
        }

        Canvas(modifier) {
            if (frameVersion == 0L || backend.isClosed) return@Canvas

            drawIntoCanvas { output ->
                val canvas = output.skiaCanvas
                if (VideoNative.requiresRenderingContext) {
                    val context = VideoNative.renderingContext(canvas.recordingContext)
                    val request = backend.revision
                    val info = backend.withHandle(null) { VideoNative.info(it, request) }
                    if (info != null && (image == null || info.serial != imageSerial || context != imageContext)) {
                        val next = backend.withHandle(null) { VideoNative.snapshot(it, request, context, info) }
                        if (next != null) {
                            image?.close()
                            image = next
                            imageSerial = info.serial
                            imageContext = context
                        }
                    }
                }

                image?.let { frame ->
                    if (size.width <= 0f || size.height <= 0f) return@let
                    val ratio = frame.width.toFloat() / frame.height
                    val width = minOf(size.width, size.height * ratio)
                    val height = width / ratio
                    canvas.drawImageRect(frame, Rect.makeWH(frame.width.toFloat(), frame.height.toFloat()), Rect.makeXYWH((size.width - width) / 2f, (size.height - height) / 2f, width, height), SamplingMode.LINEAR, paint, false)
                }
            }
        }
    }
}
