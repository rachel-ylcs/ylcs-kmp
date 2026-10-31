package love.yinlin.media

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import love.yinlin.concurrent.Lock
import love.yinlin.concurrent.atomic
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.ioContext
import love.yinlin.extension.cleaning

internal class AudioBackend(private val music: Boolean = false, private val video: Boolean = false) {
    private val lock = Lock()
    private var handle = 0L
    private var calls = 0
    private var closed = false
    private val sequence = atomic(0L)
    val events = Channel<AudioEvent>(Channel.UNLIMITED)

    val isInit: Boolean get() = lock.synchronized { !closed && handle != 0L }
    val isClosed: Boolean get() = lock.synchronized { closed }
    val revision: Long get() = sequence.value

    fun nextRequest(): Long = sequence.incrementAndGet()

    fun checkCurrent(request: Long) {
        if (isClosed || revision != request) throw CancellationException("Audio request was superseded")
    }

    suspend fun init() = Coroutines.io {
        if (isInit || isClosed) return@io
        val created = if (video) VideoNative.create(::onEvent) else AudioNative.create(music, ::onEvent)
        check(created != 0L) { "Cannot initialize audio player" }

        val retained = lock.synchronized {
            if (closed || handle != 0L) false
            else {
                handle = created
                true
            }
        }
        if (!retained) AudioNative.release(created)
    }

    private fun onEvent(revision: Long, kind: Int, value: Long, message: String?) {
        if (!isClosed) events.trySend(AudioEvent(revision, kind, value, message))
    }

    fun <T> withHandle(default: T, action: (Long) -> T): T {
        val current = lock.synchronized {
            if (closed || handle == 0L) 0L
            else {
                ++calls
                handle
            }
        }

        if (current == 0L) return default

        cleaning({
            return action(current)
        }) {
            val released = lock.synchronized {
                --calls
                if (closed && calls == 0) takeHandle() else 0L
            }
            dispose(released)
        }
    }

    private fun takeHandle(): Long {
        val previous = handle
        handle = 0L
        return previous
    }

    private fun dispose(previous: Long) {
        if (previous != 0L) CoroutineScope(ioContext).launch { AudioNative.release(previous) }
    }

    suspend fun load(path: String, request: Long): Boolean {
        require(path.isNotEmpty() && '\u0000' !in path) { "Invalid audio path" }

        checkCurrent(request)

        try {
            val loaded = Coroutines.io {
                checkCurrent(request)
                withHandle(false) { AudioNative.load(it, path, request) }
            }
            checkCurrent(request)
            return loaded
        }
        catch (cancelled: CancellationException) {
            if (!isClosed && sequence.compareAndSet(request, request + 1L)) stopLater(request + 1L)
            throw cancelled
        }
    }

    fun play(request: Long = revision): Boolean = withHandle(false) {
        AudioNative.play(it, request)
    }

    fun pause(request: Long = revision): Boolean = withHandle(false) {
        AudioNative.pause(it, request)
    }

    fun seek(position: Long, request: Long = revision): Boolean = withHandle(false) {
        AudioNative.seek(it, position.coerceAtLeast(0L), request)
    }

    fun query(): AudioSnapshot {
        val request = revision
        val value = withHandle(AudioSnapshot.Empty) { AudioNative.query(it, request) }
        return if (revision == request) value else AudioSnapshot.Empty
    }

    fun stop() {
        val request = nextRequest()
        stopLater(request)
    }

    private fun stopLater(request: Long) {
        CoroutineScope(ioContext).launch {
            withHandle(Unit) { AudioNative.stop(it, request) }
        }
    }

    suspend fun stopAndWait() {
        val request = nextRequest()
        Coroutines.io {
            withHandle(Unit) { AudioNative.stop(it, request) }
        }
    }

    fun release() {
        val previous = lock.synchronized {
            if (closed) return
            closed = true
            events.close()
            nextRequest()
            if (calls == 0) takeHandle() else 0L
        }
        dispose(previous)
    }
}
