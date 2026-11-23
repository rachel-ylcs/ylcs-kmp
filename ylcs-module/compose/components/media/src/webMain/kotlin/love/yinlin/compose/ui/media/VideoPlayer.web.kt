package love.yinlin.compose.ui.media

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ThrowableCompatible
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.PlatformViewEnvironment
import love.yinlin.compose.ui.PlatformViewHost
import love.yinlin.compose.ui.PlatformViewState
import love.yinlin.coroutines.mainImmediateContext
import love.yinlin.extension.catchingError
import love.yinlin.extension.createElement
import love.yinlin.extension.raw
import love.yinlin.foundation.PlatformContext
import love.yinlin.media.WebMediaSource
import love.yinlin.media.clearWebMedia
import love.yinlin.media.mediaErrorMessage
import love.yinlin.media.mediaMilliseconds
import org.w3c.dom.HTMLVideoElement
import kotlin.js.ExperimentalWasmJsInterop

@Stable
@OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
private class WebVideoController(topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoController(topBar, bottomBar) {
    private var request by mutableRefStateOf(WebVideoParameters())
    private val scope = CoroutineScope(SupervisorJob() + mainImmediateContext)
    private var loadJob: Job? = null
    private var mediaSource: WebMediaSource? = null
    private var resumePosition = 0L
    private var released = false

    private val viewState = object : PlatformViewState<HTMLVideoElement, WebVideoParameters>() {
        private var latestView: HTMLVideoElement? = null
        private val pendingSeek = mutableMapOf<HTMLVideoElement, Long>()

        private fun onCurrentSource(view: HTMLVideoElement, action: (HTMLVideoElement) -> Unit) = onView(view) {
            if (request.source != null && request.loadVersion == parameters.loadVersion) action(it)
        }

        override fun factory(environment: PlatformViewEnvironment): HTMLVideoElement = createElement {
            autoplay = false
            controls = true
            loop = true
            muted = false
        }

        override fun attach(view: HTMLVideoElement) {
            latestView = view

            view.onplay = {
                onCurrentSource(view) {
                    isPlaying = !it.paused
                    if (isPlaying) error = null
                    if (!it.paused && !request.playing) request = request.copy(playing = true)
                }
            }

            view.onpause = {
                onCurrentSource(view) {
                    isPlaying = !it.paused
                    if (it.paused && it.readyState > 0 && request.playing) request = request.copy(playing = false)
                }
            }

            view.ondurationchange = {
                onCurrentSource(view) {
                    duration = it.duration.mediaMilliseconds()
                }
            }

            view.ontimeupdate = {
                onCurrentSource(view) {
                    if (it.readyState <= 0) return@onCurrentSource
                    position = it.currentTime.mediaMilliseconds()
                    resumePosition = position
                }
            }

            view.onloadedmetadata = {
                onCurrentSource(view) {
                    pendingSeek.remove(it)?.let { target -> it.currentTime = target / 1000.0 }
                }
            }

            view.onerror = { message, _, _, _, failure ->
                onCurrentSource(view) {
                    error = ThrowableCompatible(failure).build() ?: Throwable(mediaErrorMessage(it) ?: message?.toString() ?: "Video playback failed")
                }
                false.raw
            }
        }

        override fun synchronize(view: HTMLVideoElement, previous: WebVideoParameters?, next: WebVideoParameters) {
            val changedSource = previous == null || previous.loadVersion != next.loadVersion || previous.source != next.source

            if (changedSource) {
                pendingSeek.remove(view)
                if (next.source == null) view.clearWebMedia()
                else {
                    view.src = next.source
                    view.load()
                }
            }

            if (next.source != null && (changedSource || previous.seekVersion != next.seekVersion)) {
                val target = if (changedSource) resumePosition else next.seekPosition
                if (view.readyState > 0) view.currentTime = target / 1000.0
                else pendingSeek[view] = target
            }

            if (changedSource || previous.commandVersion != next.commandVersion) {
                if (next.source != null && next.playing) {
                    val _ = view.play().catch { failure ->
                        onView(view) {
                            if (request.playing && request.loadVersion == next.loadVersion && request.commandVersion == next.commandVersion) {
                                error = ThrowableCompatible(failure).build() ?: Throwable("Video playback was rejected")
                            }
                        }
                        null
                    }
                }
                else view.pause()
            }
        }

        override fun detach(view: HTMLVideoElement) {
            if (latestView === view) {
                latestView = null
                if (request.source != null && view.hasAttribute("src")) resumePosition = view.currentTime.mediaMilliseconds()
            }

            pendingSeek.remove(view)

            cleanup({
                view.onplay = null
                view.onpause = null
                view.ondurationchange = null
                view.ontimeupdate = null
                view.onloadedmetadata = null
                view.onerror = null
            }, view::clearWebMedia)
        }

        override fun releaseResources() {
            latestView = null
            pendingSeek.clear()
        }
    }

    override fun releaseController() {
        if (released) return
        released = true
        scope.cancel()
        viewState.release()
        mediaSource?.release()
        mediaSource = null
        url = null
        isPlaying = false
    }

    override fun load(path: String) {
        if (released) return
        url = path
        error = null
        isPlaying = false
        position = 0L
        duration = 0L
        resumePosition = 0L
        loadJob?.cancel()
        mediaSource?.release()
        mediaSource = null
        val version = request.loadVersion + 1L
        request = request.copy(source = null, loadVersion = version, playing = true, seekPosition = 0L)

        loadJob = scope.launch {
            val failure = catchingError {
                val source = WebMediaSource.resolve(path)
                if (released || version != request.loadVersion) source.release()
                else {
                    mediaSource = source
                    request = request.copy(source = source.url)
                }
            }
            if (failure != null && version == request.loadVersion) error = failure
        }
    }

    override fun play() {
        if (!released) request = request.copy(playing = true, commandVersion = request.commandVersion + 1L)
    }

    override fun pause() {
        if (!released) request = request.copy(playing = false, commandVersion = request.commandVersion + 1L)
    }

    override fun stop() {
        if (released) return

        loadJob?.cancel()
        mediaSource?.release()
        mediaSource = null
        url = null
        isPlaying = false
        position = 0L
        duration = 0L
        resumePosition = 0L
        request = request.copy(source = null, loadVersion = request.loadVersion + 1L, playing = false, seekPosition = 0L)
    }

    override fun seek(position: Long) {
        if (released) return
        val target = position.coerceAtLeast(0L)
        resumePosition = target
        request = request.copy(seekPosition = target, seekVersion = request.seekVersion + 1L)
    }

    @Composable
    override fun SurfaceContent(modifier: Modifier) {
        PlatformViewHost(viewState, request, modifier)
    }
}

actual fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory,
    bottomBar: VideoActionBar.Factory
): VideoController = WebVideoController(topBar, bottomBar)
