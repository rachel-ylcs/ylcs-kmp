package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import kotlinx.coroutines.isActive
import love.yinlin.compose.extension.rememberRefState

@Composable
actual fun PAGView(
    state: PAGState,
    modifier: Modifier,
    composition: PAGComposition?,
    isPlaying: Boolean,
    config: PAGConfig,
) {
    val player: PAGPlayer = remember { PAGPlayer() }
    //var surface: PAGSurface? by rememberRefState { null }
    var painter: BitmapPainter? by rememberRefState { null }

    val (contentScale, alignment) = remember(config) {
        when (config.scaleMode) {
            PAGScaleMode.None -> ContentScale.None to Alignment.TopStart
            PAGScaleMode.Stretch -> ContentScale.Fit to Alignment.Center
            PAGScaleMode.LetterBox -> ContentScale.FillBounds to Alignment.Center
            PAGScaleMode.Zoom -> ContentScale.Crop to Alignment.Center
        }
    }

    Layout(modifier = modifier.then(painter?.let {
        Modifier.paint(
            painter = it,
            contentScale = contentScale,
            alignment = alignment,
        )
    } ?: Modifier)) { _, constraints ->
        val width = if (constraints.hasFixedWidth) constraints.maxWidth else painter?.intrinsicSize?.width?.toInt() ?: 0
        val height = if (constraints.hasFixedHeight) constraints.maxHeight else painter?.intrinsicSize?.height?.toInt() ?: 0
        layout(width, height) {}
    }

    LaunchedEffect(config) {
        config.scaleMode.let { if (player.scaleMode != it) player.scaleMode = it }
        config.cachedEnabled?.let { if (player.cacheEnabled != it) player.cacheEnabled = it }
        config.cacheScale?.let { if (player.cacheScale != it) player.cacheScale = it }
        config.maxFrameRate?.let { if (player.maxFrameRate != it) player.maxFrameRate = it }
        config.videoEnabled?.let { if (player.videoEnabled != it) player.videoEnabled = it }
        config.useDiskCache?.let { if (player.useDiskCache != it) player.useDiskCache = it }
    }

    DisposableEffect(composition) {
        player.composition = composition
        if (composition != null) player.surface = PAGSurface.makeOffscreen(composition.width, composition.height)
        onDispose { player.surface?.close() }
    }

    LaunchedEffect(state.flushFlag) {
        player.flush()
    }

    LaunchedEffect(isPlaying) {
        val duration = composition?.duration
        if (isPlaying && duration != null) {
            var lastRenderingTime = 0L
            while (isActive) {
                withFrameMillis { currentRenderingTime ->
                    val currentProgress = player.progress
                    state.stateProgress = currentProgress
                    painter = state.makeSnapshot()?.let(::BitmapPainter)
                    if (lastRenderingTime != 0L) {
                        val delta = (currentRenderingTime - lastRenderingTime) * 1000.0 / duration
                        player.progress += delta
                        player.flush()
                    }
                    lastRenderingTime = currentRenderingTime
                }
            }
        }
    }

    DisposableEffect(player) {
        state.player = player
        onDispose {
            state.player = null
            player.close()
        }
    }
}