@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui.media

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import love.yinlin.compose.ui.PlatformViewEnvironment
import love.yinlin.compose.ui.PlatformViewHost
import love.yinlin.compose.ui.PlatformViewState
import love.yinlin.extension.catchingError
import love.yinlin.foundation.PlatformContext
import love.yinlin.media.IOSAVPlayer
import love.yinlin.media.IOSPlayerLifetime
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CATransaction
import platform.UIKit.*
import kotlin.time.Duration.Companion.milliseconds

private class VideoView : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val videoLayer = AVPlayerLayer()

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        videoLayer.frame = bounds
        CATransaction.commit()
    }
}

@Stable
private data class VideoViewParameters(val player: AVPlayer?)

@Stable
private class VideoViewState : PlatformViewState<VideoView, VideoViewParameters>() {
    override val properties = UIKitInteropProperties(interactionMode = null)

    override fun factory(environment: PlatformViewEnvironment): VideoView {
        val videoView = VideoView()
        videoView.backgroundColor = UIColor.blackColor
        videoView.videoLayer.videoGravity = AVLayerVideoGravityResizeAspect
        videoView.layer.addSublayer(videoView.videoLayer)
        return videoView
    }

    override fun synchronize(view: VideoView, previous: VideoViewParameters?, next: VideoViewParameters) {
        if (previous?.player !== next.player) view.videoLayer.player = next.player
    }

    override fun detach(view: VideoView) {
        view.videoLayer.player = null
    }
}

@Stable
private class IOSVideoController(topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoController(topBar, bottomBar) {
    private val viewState = VideoViewState()
    private val lifetime = IOSPlayerLifetime()
    private val scope = lifetime.scope
    private var player: IOSAVPlayer? = null
    private var surfacePlayer: AVPlayer? by mutableStateOf(null)
    private var progressJob: Job? = null

    private fun ensurePlayer(): IOSAVPlayer = player ?: IOSAVPlayer(
        lifetime = lifetime,
        keepSessionAtEnd = true,
        onChanged = ::onPlaybackChanged,
        onEnded = { generation ->
            val current = player
            if (!lifetime.isClosed && current != null && current.generation == generation && current.wantsPlay) {
                current.seek(0L)
            }
        },
    ).also {
        player = it
        surfacePlayer = it.nativePlayer
    }

    private fun synchronize() {
        if (lifetime.isClosed) return
        val current = player ?: return
        surfacePlayer = current.nativePlayer
        isPlaying = current.isPlaying
        position = current.position
        duration = current.duration
        error = current.lastError
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
                while (isActive && !lifetime.isClosed) {
                    val current = player ?: break
                    if (!current.wantsPlay) break
                    current.checkForFailure()
                    if (!isActive || lifetime.isClosed) break
                    synchronize()
                    delay(100.milliseconds)
                }
            }?.let { failure ->
                player?.pause()
                error = failure
            }
        }
    }

    private fun command(block: () -> Unit) {
        scope.launch(Dispatchers.Main.immediate) {
            if (lifetime.isClosed) return@launch
            catchingError(block)?.let { failure ->
                player?.pause()
                error = failure
            }
        }
    }

    override fun load(path: String) = command {
        val current = ensurePlayer()
        url = if (current.load(path, playing = true)) path else null
        synchronize()
    }

    override fun play() = command { player?.play() }
    override fun pause() = command { player?.pause() }
    override fun stop() = command {
        player?.stop()
        url = null
    }
    override fun seek(position: Long) = command { player?.seek(position) }

    override fun releaseController() {
        viewState.release()
        lifetime.close {
            progressJob?.cancel()
            progressJob = null
            player?.release()
            player = null
            surfacePlayer = null
            url = null
            isPlaying = false
            position = 0L
            duration = 0L
            error = null
        }
    }

    @Composable
    override fun SurfaceContent(modifier: Modifier) {
        PlatformViewHost(viewState, VideoViewParameters(surfacePlayer), modifier)
    }
}

actual fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory,
    bottomBar: VideoActionBar.Factory
): VideoController = IOSVideoController(topBar, bottomBar)
