package love.yinlin.media.lyrics

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.window.FloatingView
import love.yinlin.startup.StartupMusicPlayer

@Stable
actual class FloatingLyrics actual constructor(val startup: StartupMusicPlayer) {
    actual var isAttached: Boolean by mutableStateOf(false)
        private set

    private val view = object : FloatingView() {
        override val touchable: Boolean = false

        override fun onAttached() { isAttached = true }
        override fun onDetached() { isAttached = false }
        @Composable
        override fun Content() {
            if (isAttached && startup.isInit) {
                app.ComposedLayout(
                    modifier = Modifier.fillMaxSize(),
                    bgColor = Colors.Transparent
                ) {
                    Layout(
                        modifier = Modifier.fillMaxSize(),
                        content = {
                            if (startup.isPlaying) {
                                startup.engine.FloatingLyricsCanvas(modifier = Modifier.fillMaxWidth(), config = app.config.lyricsEngineConfig, textStyle = Theme.typography.v6.bold)
                            }
                        }
                    ) { measurables, constraints ->
                        val config = app.config.lyricsEngineConfig
                        val maxWidth = constraints.maxWidth
                        val maxHeight = constraints.maxHeight
                        val start = (maxWidth * config.android.left.coerceIn(0f, 1f)).toInt()
                        val end = (maxWidth * (1 - config.android.right).coerceIn(0f, 1f)).toInt()
                        val top = (maxHeight * 0.2f * config.android.top.coerceIn(0f, 1f)).toInt()
                        val childWidth = (maxWidth - start - end).coerceAtLeast(0)
                        val placeable = measurables.firstOrNull()?.measure(constraints.copy(
                            minWidth = childWidth,
                            maxWidth = childWidth,
                            minHeight = 0
                        ))
                        val childHeight = placeable?.height ?: 0
                        layout(maxWidth, maxHeight) {
                            placeable?.placeRelative(start, if (config.android.placeTop) top else maxHeight - top - childHeight)
                        }
                    }
                }
            }
        }
    }

    actual fun attach() {
        startup.context.activity?.let { activity ->
            view.attach(activity) { app.config.enabledFloatingLyrics = false }
        }
    }

    actual fun detach() {
        startup.context.activity?.let { activity ->
            view.detach(activity)
        }
    }

    actual suspend fun initDelay() {
        if (app.config.enabledFloatingLyrics && !isAttached) attach()
    }
}