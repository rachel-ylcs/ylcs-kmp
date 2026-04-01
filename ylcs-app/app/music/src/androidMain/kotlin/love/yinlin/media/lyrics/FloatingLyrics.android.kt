package love.yinlin.media.lyrics

import android.view.Gravity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalWindowInfo
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.window.FloatingView
import love.yinlin.startup.StartupMusicPlayer

@Stable
actual class FloatingLyrics actual constructor(val mp: StartupMusicPlayer) {
    actual var isAttached: Boolean by mutableStateOf(false)
        private set

    private val view = object : FloatingView() {
        override val touchable: Boolean = false

        override fun onAttached() { isAttached = true }

        override fun onDetached() { isAttached = false }

        @Composable
        override fun Content() {
            if (isAttached && mp.isInit) {
                val screenHeight = LocalWindowInfo.current.containerSize.height
                val lyricsTopOffset by rememberDerivedState {
                    val config = app.config.lyricsEngineConfig
                    config.android.placeTop to config.android.top
                }

                LaunchedEffect(screenHeight, lyricsTopOffset) {
                    val topOffset = screenHeight * 0.2f * lyricsTopOffset.second.coerceIn(0f, 1f)
                    val gravity = (if (lyricsTopOffset.first) Gravity.TOP else Gravity.BOTTOM) or Gravity.START
                    updateLayoutParams(gravity, Offset(0f, topOffset))
                }

                app.ComposedLayout(
                    modifier = Modifier.fillMaxWidth(),
                    bgColor = Colors.Transparent
                ) {
                    Layout(
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            if (mp.isPlaying) {
                                mp.engine.FloatingLyricsCanvas(modifier = Modifier.fillMaxWidth(), config = app.config.lyricsEngineConfig, textStyle = Theme.typography.v6.bold)
                            }
                        }
                    ) { measurables, constraints ->
                        val config = app.config.lyricsEngineConfig
                        val maxWidth = constraints.maxWidth
                        val start = (maxWidth * config.android.left.coerceIn(0f, 1f)).toInt()
                        val end = (maxWidth * (1 - config.android.right).coerceIn(0f, 1f)).toInt()

                        val childWidth = (maxWidth - start - end).coerceAtLeast(0)
                        val placeable = measurables.firstOrNull()?.measure(constraints.copy(
                            minWidth = childWidth,
                            maxWidth = childWidth,
                            minHeight = 0
                        ))
                        layout(maxWidth, placeable?.height ?: 0) {
                            placeable?.placeRelative(start, 0)
                        }
                    }
                }
            }
        }
    }

    actual fun attach() {
        mp.pool.activity?.let { activity ->
            view.attach(activity) { app.config.enabledFloatingLyrics = false }
        }
    }

    actual fun detach() = view.detach()

    actual suspend fun initDelay() {
        if (app.config.enabledFloatingLyrics && !isAttached) attach()
    }
}