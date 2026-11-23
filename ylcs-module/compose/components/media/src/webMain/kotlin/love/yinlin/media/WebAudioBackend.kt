@file:OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
package love.yinlin.media

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ThrowableCompatible
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.cleaning
import love.yinlin.extension.createElement
import love.yinlin.extension.raw
import org.w3c.dom.HTMLAudioElement

internal class WebAudioBackend(
    private val onStateChanged: () -> Unit = {},
    private val onError: (Throwable) -> Unit = {},
    private val onEnded: (Long) -> Unit,
) {
    private class Playback(val source: WebMediaSource) {
        val element = createElement<HTMLAudioElement> { preload = "auto" }
        val ready = CompletableDeferred<Unit>()
        var playAttempt = 0L
    }

    private var playback: Playback? = null
    private var closed = false
    private var wantsPlay = false
    private var pendingSeek: Long? = null
    var revision = 0L
        private set
    var isInit = false
        private set

    val isPlaying: Boolean get() = playback?.element?.let { !it.paused && !it.ended } == true
    val position: Long get() = playback?.element?.currentTime?.mediaMilliseconds() ?: 0L
    val duration: Long get() = playback?.element?.duration?.mediaMilliseconds() ?: 0L

    fun init() { if (!closed) isInit = true }

    suspend fun load(playing: Boolean, resolve: suspend () -> WebMediaSource) = Coroutines.mainImmediate {
        check(isInit) { "Audio player is not initialized" }
        val request = ++revision
        wantsPlay = playing
        pendingSeek = null
        clearPlayback()
        val source = resolve()
        var current: Playback? = null
        var loaded = false
        cleaning({
            currentCoroutineContext().ensureActive()
            if (revision != request) throw CancellationException("Audio load superseded")
            val session = Playback(source)
            current = session
            playback = session
            bind(session, request)
            session.element.src = source.url
            session.ready.await()
            if (revision != request) throw CancellationException("Audio load superseded")
            pendingSeek?.let { applySeek(session, it) }
            pendingSeek = null
            if (wantsPlay) start(session)
            loaded = true
            onStateChanged()
        }) {
            if (!loaded) {
                if (current == null) source.release()
                else if (playback === current) clearPlayback()
            }
        }
    }

    private fun bind(session: Playback, request: Long) = with(session.element) {
        onloadedmetadata = { session.ready.complete(Unit) }
        ondurationchange = { if (playback === session) onStateChanged() }
        onplay = { if (playback === session) onStateChanged() }
        onpause = { if (playback === session) onStateChanged() }
        onseeked = { if (playback === session) onStateChanged() }
        onended = {
            if (playback === session) {
                onStateChanged()
                onEnded(request)
            }
        }
        onerror = { message, _, _, _, failure ->
            if (playback === session) {
                val error = ThrowableCompatible(failure).build()
                    ?: IllegalStateException(mediaErrorMessage(this) ?: message?.toString() ?: "Audio playback failed")
                if (!session.ready.completeExceptionally(error)) onError(error)
            }
            false.raw
        }
    }

    private fun start(session: Playback) {
        val attempt = ++session.playAttempt
        val _ = session.element.play().catch { failure ->
            if (playback === session && attempt == session.playAttempt && wantsPlay && session.element.paused) {
                onStateChanged()
                onError(ThrowableCompatible(failure).build() ?: IllegalStateException("Audio playback was rejected"))
            }
            null
        }
    }

    fun play() {
        wantsPlay = true
        playback?.let { if (it.element.readyState > 0) start(it) }
    }

    fun pause() {
        wantsPlay = false
        playback?.let {
            it.playAttempt++
            it.element.pause()
        }
    }

    fun seekTo(position: Long) {
        val target = position.coerceAtLeast(0L)
        val session = playback
        if (session == null || session.element.readyState <= 0) pendingSeek = target
        else applySeek(session, target)
        play()
    }

    private fun applySeek(session: Playback, position: Long) {
        session.element.currentTime = (if (duration > 0) position.coerceAtMost(duration) else position) / 1000.0
    }

    fun stop() {
        revision++
        wantsPlay = false
        pendingSeek = null
        clearPlayback()
    }

    private fun clearPlayback() {
        val session = playback ?: return
        playback = null
        session.ready.cancel()
        with(session.element) {
            onloadedmetadata = null
            ondurationchange = null
            onplay = null
            onpause = null
            onseeked = null
            onended = null
            onerror = null
            cleaning({ clearWebMedia() }, session.source::release)
        }
        onStateChanged()
    }

    fun release() {
        closed = true
        isInit = false
        stop()
    }
}
