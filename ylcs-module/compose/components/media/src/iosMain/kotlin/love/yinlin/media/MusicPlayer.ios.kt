package love.yinlin.media

import androidx.compose.runtime.Stable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingError
import love.yinlin.foundation.PlatformContext
import platform.Foundation.*
import platform.MediaPlayer.*
import platform.UIKit.*
import kotlin.time.Duration.Companion.milliseconds

@Stable
@OptIn(ExperimentalForeignApi::class)
class IOSMusicPlayer(fetcher: MediaMetadataFetcher) : CommonMusicPlayer(fetcher) {
    private companion object {
        // 系统锁屏信息和遥控命令也是应用级资源，只交给一个 MusicPlayer。
        var remoteOwner: IOSMusicPlayer? = null
    }

    private data class CommandBinding(
        val command: MPRemoteCommand,
        val target: Any,
        val previouslyEnabled: Boolean,
        val requiresDuration: Boolean = false,
    )

    private val lifetime = IOSPlayerLifetime()
    private val scope = lifetime.scope
    private var player: IOSAVPlayer? = null
    private var progressJob: Job? = null
    private val commandBindings: MutableList<CommandBinding> = []
    private var receivingRemoteEvents = false
    private var lastNotifiedPosition = -1L
    private var metadata = mutableMapOf<Any?, Any?>()
    private var coverTask: NSURLSessionDataTask? = null
    private var coverVersion = 0L

    private fun synchronize() {
        if (lifetime.isClosed) return
        val current = player ?: return
        isPlaying = current.isPlaying
        position = current.position
        duration = current.duration
        error = current.lastError
        if (position != lastNotifiedPosition) {
            lastNotifiedPosition = position
            listener?.onPositionChanged(position)
        }
        updateNowPlaying()
    }

    private fun onPlaybackChanged() {
        if (lifetime.isClosed) return
        synchronize()
        updateProgressJob()
    }

    private fun updateProgressJob() {
        if (lifetime.isClosed || player?.wantsPlay != true) {
            progressJob?.cancel()
            progressJob = null
            return
        }
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            catchingError {
                while (Coroutines.isActive() && !lifetime.isClosed) {
                    val current = player ?: break
                    if (!current.wantsPlay) break
                    current.checkForFailure()
                    if (!Coroutines.isActive() || lifetime.isClosed) break
                    synchronize()
                    delay(fetcher.interval.milliseconds)
                }
            }?.let { failure ->
                player?.pause()
                error = failure
                updateNowPlaying()
            }
        }
    }

    private fun onTrackEnded(generation: Long) {
        scope.launch(Dispatchers.Main.immediate) {
            val current = player ?: return@launch
            if (lifetime.isClosed || current.generation != generation || !current.wantsPlay) return@launch
            catchingError {
                if (!isReady || currentIndex !in musicList.indices) internalStop()
                else {
                    val index = when (playMode) {
                        MediaPlayMode.Loop -> currentIndex
                        MediaPlayMode.Random -> randomNextIndex ?: reshuffled()
                        else -> loopNextIndex
                    }
                    internalGotoIndex(index)
                }
            }?.let { failure ->
                current.pause()
                error = failure
                updateNowPlaying()
            }
        }
    }

    private fun installRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        bind(center.playCommand) { play() }
        bind(center.pauseCommand) { pause() }
        bind(center.togglePlayPauseCommand) {
            if (player?.wantsPlay == true) pause() else play()
        }
        bind(center.stopCommand) { stop() }
        bind(center.previousTrackCommand) { gotoPrevious() }
        bind(center.nextTrackCommand) { gotoNext() }
        val command = center.changePlaybackPositionCommand
        val previouslyEnabled = command.enabled
        command.enabled = false
        val target = command.addTargetWithHandler { event ->
            val seconds = (event as? MPChangePlaybackPositionCommandEvent)?.positionTime
            if (lifetime.isClosed || seconds == null || !seconds.isFinite()) {
                MPRemoteCommandHandlerStatusCommandFailed
            } else {
                runRemote { seekTo((seconds.coerceAtLeast(0.0) * 1000.0).toLong()) }
                MPRemoteCommandHandlerStatusSuccess
            }
        }
        commandBindings += CommandBinding(command, target, previouslyEnabled, requiresDuration = true)
    }

    private fun bind(command: MPRemoteCommand, action: suspend () -> Unit) {
        val previouslyEnabled = command.enabled
        command.enabled = false
        val target = command.addTargetWithHandler {
            if (lifetime.isClosed) MPRemoteCommandHandlerStatusCommandFailed
            else {
                runRemote(action)
                MPRemoteCommandHandlerStatusSuccess
            }
        }
        commandBindings += CommandBinding(command, target, previouslyEnabled)
    }

    private fun runRemote(action: suspend () -> Unit) {
        scope.launch(Dispatchers.Main.immediate) {
            if (lifetime.isClosed || !isInit) return@launch
            catchingError {
                action()
            }?.let { error = it }
        }
    }

    private fun removeRemoteCommands() {
        commandBindings.forEach { (val command, val target, val previouslyEnabled) ->
            command.removeTarget(target)
            command.enabled = previouslyEnabled
        }
        commandBindings.clear()
        if (receivingRemoteEvents) {
            UIApplication.sharedApplication.endReceivingRemoteControlEvents()
            receivingRemoteEvents = false
        }
        if (remoteOwner === this) {
            MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
            remoteOwner = null
        }
    }

    private fun loadMetadata(id: String) {
        val info = runCatching { fetcher.extractMetadata(id) }.getOrNull()
        metadata[MPMediaItemPropertyTitle] = info?.name ?: id
        info?.let {
            metadata[MPMediaItemPropertyArtist] = it.singer
            metadata[MPMediaItemPropertyAlbumTitle] = it.album
            metadata[MPMediaItemPropertyComposer] = it.composer
        }
        updateNowPlaying()

        val path = runCatching { fetcher.extractCoverUri(id) }.getOrNull() ?: return
        val url = iosMediaURL(path) ?: return
        val expectedVersion = coverVersion
        if (url.isFileURL()) {
            scope.launch {
                val data = Coroutines.io { NSData.dataWithContentsOfURL(url) }
                if (!lifetime.isClosed && currentId == id && coverVersion == expectedVersion) installCover(data)
            }
        } else {
            val task = NSURLSession.sharedSession.dataTaskWithURL(url) { data, response, failure ->
                scope.launch(Dispatchers.Main.immediate) {
                    if (lifetime.isClosed || currentId != id || coverVersion != expectedVersion) return@launch
                    coverTask = null
                    val status = (response as? NSHTTPURLResponse)?.statusCode ?: 0L
                    if (failure == null && status in 200L .. 299L) installCover(data)
                }
            }
            task.resume()
            coverTask = task
        }
    }

    private fun installCover(data: NSData?) {
        if (data == null || data.length == 0UL) return
        val image = UIImage.imageWithData(data) ?: return
        metadata[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(image.size) { image }
        updateNowPlaying()
    }

    private fun cancelCover() {
        coverVersion++
        coverTask?.cancel()
        coverTask = null
    }

    private fun updateNowPlaying() {
        if (remoteOwner !== this || lifetime.isClosed) return
        val ready = currentId != null && isReady
        commandBindings.forEach { (val command, val requiresDuration) ->
            command.enabled = ready && (!requiresDuration || duration > 0L)
        }
        if (!ready) {
            MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
            return
        }
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = metadata.toMutableMap().apply {
            this[MPMediaItemPropertyTitle] = metadata[MPMediaItemPropertyTitle] ?: currentId
            this[MPNowPlayingInfoPropertyElapsedPlaybackTime] = position / 1000.0
            this[MPNowPlayingInfoPropertyPlaybackRate] = if (isPlaying) 1.0 else 0.0
            this[MPNowPlayingInfoPropertyDefaultPlaybackRate] = 1.0
            if (duration > 0L) this[MPMediaItemPropertyPlaybackDuration] = duration / 1000.0
        }
    }

    override suspend fun init(context: PlatformContext) = Coroutines.main {
        if (lifetime.isClosed || isInit) return@main
        catchingError {
            check(remoteOwner == null || remoteOwner === this@IOSMusicPlayer) { "只能有一个 MusicPlayer 管理系统媒体遥控" }
            remoteOwner = this@IOSMusicPlayer
            player = IOSAVPlayer(
                lifetime = lifetime,
                mixWithOthers = !fetcher.audioFocus,
                backgroundMusic = true,
                keepSessionAtEnd = true,
                onChanged = ::onPlaybackChanged,
                onEnded = ::onTrackEnded,
            )
            installRemoteCommands()
            UIApplication.sharedApplication.beginReceivingRemoteControlEvents()
            receivingRemoteEvents = true
            isInit = true
            error = null
        }?.let { failure ->
            player?.release()
            player = null
            removeRemoteCommands()
            isInit = false
            error = failure
        } ?: Unit
    }

    override suspend fun play() = Coroutines.main {
        if (!lifetime.isClosed && isInit && isReady) player?.play()
    }

    override suspend fun pause() = Coroutines.main {
        if (!lifetime.isClosed) player?.pause()
    }

    override suspend fun seekTo(position: Long) = Coroutines.main {
        if (!lifetime.isClosed && isInit && isReady) player?.seek(position)
    }

    override suspend fun innerStop() = Coroutines.main {
        if (lifetime.isClosed) return@main
        cancelCover()
        metadata.clear()
        player?.stop(clearError = false)
        currentId = null
        isPlaying = false
        position = 0L
        duration = 0L
        updateNowPlaying()
    }

    override suspend fun innerGotoIndex(path: String, playing: Boolean): Boolean =
        Coroutines.main {
            if (lifetime.isClosed || !isInit) return@main false
            val current = player ?: return@main false
            val id = musicList.getOrNull(currentIndex) ?: return@main false
            cancelCover()
            metadata.clear()
            currentId = id
            lastNotifiedPosition = -1L
            if (!current.load(path, playing)) {
                error = current.lastError
                return@main false
            }
            error = current.lastError
            listener?.onMusicChanged(id)
            loadMetadata(id)
            true
        }


    override fun release() {
        lifetime.close {
            progressJob?.cancel()
            progressJob = null
            cancelCover()
            player?.release()
            player = null
            removeRemoteCommands()
            metadata.clear()
            musicList.clear()
            currentIndex = -1
            resetShuffled()
            currentId = null
            isInit = false
            isPlaying = false
            position = 0L
            duration = 0L
            error = null
            listener = null
        }
    }
}

actual fun buildMusicPlayer(fetcher: MediaMetadataFetcher): MusicPlayer = IOSMusicPlayer(fetcher)