package love.yinlin.compose.ui.media

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ThrowableCompatible
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.PlatformViewEnvironment
import love.yinlin.compose.ui.PlatformViewHost
import love.yinlin.compose.ui.PlatformViewState
import love.yinlin.extension.createElement
import love.yinlin.extension.raw
import love.yinlin.foundation.PlatformContext
import org.w3c.dom.HTMLVideoElement
import kotlin.js.ExperimentalWasmJsInterop

@Stable
private data class WebVideoParameters(
    val source: String? = null,
    val loadVersion: Long = 0L,
    val playing: Boolean = false,
    val commandVersion: Long = 0L,
    val seekPosition: Long = 0L,
    val seekVersion: Long = 0L,
)

@Stable
@OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
private class WebVideoController(topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoController(topBar, bottomBar) {
    private var request by mutableRefStateOf(WebVideoParameters())
    private var resumePosition = 0L
    private var released = false
    private val viewState = object : PlatformViewState<HTMLVideoElement, WebVideoParameters>() {
        private var latestView: HTMLVideoElement? = null
        private val pendingSeek = mutableMapOf<HTMLVideoElement, Long>()

        override fun factory(environment: PlatformViewEnvironment): HTMLVideoElement = createElement {
            autoplay = false
            controls = true
            loop = true
            muted = false
        }

        override fun attach(view: HTMLVideoElement) {
            latestView = view
            view.onplay = {
                onView(view) {
                    isPlaying = !it.paused
                    if (!it.paused && !request.playing) request = request.copy(playing = true)
                }
            }
            view.onpause = {
                onView(view) {
                    isPlaying = !it.paused
                    if (it.paused && request.playing) request = request.copy(playing = false)
                }
            }
            view.ondurationchange = { onView(view) { duration = (it.duration.takeIf(Double::isFinite) ?: 0.0).times(1000).toLong() } }
            view.ontimeupdate = {
                onView(view) {
                    position = (it.currentTime * 1000).toLong()
                    resumePosition = position
                }
            }
            view.onloadedmetadata = {
                onView(view) { pendingSeek.remove(it)?.let { target -> it.currentTime = target / 1000.0 } }
            }
            view.onerror = { message, _, _, _, failure ->
                onView(view) { error = ThrowableCompatible(failure).build() ?: Throwable(message?.toString() ?: "error") }
                false.raw
            }
        }

        override fun synchronize(view: HTMLVideoElement, previous: WebVideoParameters?, next: WebVideoParameters) {
            val changedSource = previous == null || previous.loadVersion != next.loadVersion
            if (changedSource) {
                pendingSeek.remove(view)
                if (next.source == null) view.removeAttribute("src")
                else view.src = next.source
                view.load()
            }
            if (next.source != null && (changedSource || previous.seekVersion != next.seekVersion)) {
                val target = if (previous == null) resumePosition else next.seekPosition
                if (view.readyState > 0) view.currentTime = target / 1000.0
                else pendingSeek[view] = target
            }
            if (changedSource || previous.commandVersion != next.commandVersion) {
                if (next.source != null && next.playing) {
                    val _ = view.play().catch { failure ->
                        onView(view) {
                            if (request.playing && parameters.loadVersion == next.loadVersion && parameters.commandVersion == next.commandVersion) {
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
                resumePosition = (view.currentTime * 1000).toLong()
            }
            pendingSeek.remove(view)
            cleanup({
                view.onplay = null
                view.onpause = null
                view.ondurationchange = null
                view.ontimeupdate = null
                view.onloadedmetadata = null
                view.onerror = null
            }, view::pause, { view.removeAttribute("src") }, view::load)
        }

        override fun releaseResources() {
            latestView = null
            pendingSeek.clear()
        }
    }

    override fun releaseController() {
        if (released) return
        released = true
        viewState.release()
        url = null
        isPlaying = false
    }

    override fun load(path: String) {
        if (released) return
        url = path
        error = null
        position = 0L
        duration = 0L
        resumePosition = 0L
        request = request.copy(source = path, loadVersion = request.loadVersion + 1L, playing = true, seekPosition = 0L)
    }

    override fun play() {
        if (!released) request = request.copy(playing = true, commandVersion = request.commandVersion + 1L)
    }

    override fun pause() {
        if (!released) request = request.copy(playing = false, commandVersion = request.commandVersion + 1L)
    }

    override fun stop() {
        if (released) return
        url = null
        position = 0L
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
