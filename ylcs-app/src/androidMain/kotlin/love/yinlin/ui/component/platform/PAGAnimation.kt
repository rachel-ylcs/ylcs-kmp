package love.yinlin.ui.component.platform

import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.extension.OffScreenEffect
import love.yinlin.extension.rememberState
import love.yinlin.ui.CustomUI
import org.libpag.*

@Composable
actual fun PAGImageAnimation(
    data: ByteArray,
    repeatCount: Int,
    renderScale: Float,
    scaleMode: PAGConfig.ScaleMode,
    cacheAllFramesInMemory: Boolean,
    modifier: Modifier,
) {
    val state: MutableState<PAGImageView?> = rememberState { null }

    CustomUI(
        view = state,
        factory = { context ->
            PAGImageView(context)
        },
        update = { view ->
            if (view.repeatCount() != repeatCount) view.setRepeatCount(repeatCount)
            view.setScaleMode(when (scaleMode) {
                PAGConfig.ScaleMode.None -> PAGScaleMode.None
                PAGConfig.ScaleMode.Stretch -> PAGScaleMode.Stretch
                PAGConfig.ScaleMode.LetterBox -> PAGScaleMode.LetterBox
                PAGConfig.ScaleMode.Zoom -> PAGScaleMode.Zoom
            })
            view.setRenderScale(renderScale)
            if (view.cacheAllFramesInMemory() != cacheAllFramesInMemory) view.setCacheAllFramesInMemory(cacheAllFramesInMemory)
        },
        modifier = modifier
    )

    LaunchedEffect(data) {
        state.value?.let { view ->
            view.composition = PAGFile.Load(data)
        }
    }

    OffScreenEffect { isForeground ->
        state.value?.let { view ->
            if (isForeground) view.play()
            else view.pause()
        }
    }
}

@Stable
actual class PAGState {
    actual var data: ByteArray by mutableStateOf(ByteArray(0))
    actual var progress: Double by mutableDoubleStateOf(0.0)

    var player: PAGPlayer? = null
    val surface = mutableStateOf<TextureView?>(null)

    fun releasePlayer() {
        player?.apply {
            surface?.release()
            surface = null
            composition = null
            release()
        }
        player = null
    }
}

@Composable
actual fun PAGAnimation(
    state: PAGState,
    modifier: Modifier
) {
    CustomUI(
        view = state.surface,
        factory = { context ->
            state.releasePlayer()
            state.player = PAGPlayer().apply {
                setCacheEnabled(true)
                setUseDiskCache(true)
                setScaleMode(PAGScaleMode.LetterBox)
            }
            TextureView(context).apply {
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        state.player?.surface = PAGSurface.FromSurfaceTexture(surface)
                    }
                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        update = {
            state.player?.composition = PAGFile.Load(state.data)
        },
        release = { _, onRelease ->
            state.releasePlayer()
            onRelease()
        },
        modifier = modifier
    )

    LaunchedEffect(state.progress) {
        state.player?.let {
            it.progress = state.progress
            it.flush()
        }
    }
}