package love.yinlin.ui.component.platform

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
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

    val player = PAGPlayer()
}

@Composable
actual fun PAGAnimation(
    state: PAGState,
    modifier: Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val (width, height) = with(LocalDensity.current) { maxWidth.roundToPx() to maxHeight.roundToPx() }

        LaunchedEffect(width, height) {
            state.player.surface = PAGSurface.MakeOffscreen(width, height)
        }

        LaunchedEffect(state.data) {
            try {
                state.player.composition = PAGFile.Load(state.data)
            }
            catch (e: Throwable) {
                println(e.stackTraceToString())
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                state.player.apply {
                    surface?.release()
                    surface = null
                    composition = null
                    release()
                }
            }
        }

        var bitmap: ImageBitmap? by rememberState { null }

        LaunchedEffect(state.progress) {
            state.player.progress = state.progress
            state.player.flush()
            state.player.surface?.makeSnapshot()?.let {
                bitmap = it.asImageBitmap()
            }
        }

        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}