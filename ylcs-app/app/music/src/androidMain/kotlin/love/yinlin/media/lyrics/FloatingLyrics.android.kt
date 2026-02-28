package love.yinlin.media.lyrics

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.window.FloatingView
import love.yinlin.extension.lazyProvider
import love.yinlin.foundation.Context
import love.yinlin.startup.StartupMusicPlayer

@Stable
actual class FloatingLyrics {
    private val mp by lazyProvider { app.startup<StartupMusicPlayer>() }

    private lateinit var activity: ComponentActivity

    actual var isAttached: Boolean by mutableStateOf(false)
        private set

    private val view = object : FloatingView() {
        override fun onAttached() { isAttached = true }
        override fun onDetached() { isAttached = false }
        @Composable
        override fun Content() {
            if (isAttached) this@FloatingLyrics.Content()
        }
    }

    actual fun attach() = view.attach(activity) { app.config.enabledFloatingLyrics = false }

    actual fun detach() = view.detach(activity)

    actual suspend fun initDelay(context: Context) {
        activity = context.activity
        if (app.config.enabledFloatingLyrics && !isAttached) attach()
    }

    actual fun update() = Unit

    @Composable
    actual fun Content() {
        mp?.let { player ->
            if (player.isPlaying) {
                app.ComposedLayout(
                    modifier = Modifier.fillMaxWidth(),
                    bgColor = Colors.Transparent
                ) {
                    val config = app.config.lyricsEngineConfig
                    Box(modifier = Modifier.fillMaxWidth().layout { measurable, constraints ->
                        val maxWidth = constraints.maxWidth
                        val start = (maxWidth * config.android.left.coerceIn(0f, 1f)).toInt()
                        val end = (maxWidth * (1 - config.android.right).coerceIn(0f, 1f)).toInt()
                        val top = (config.android.top * 30.dp.roundToPx()).toInt()
                        val childWidth = (maxWidth - start - end).coerceAtLeast(0)
                        val placeable = measurable.measure(constraints.copy(minWidth = childWidth, maxWidth = childWidth))
                        layout(maxWidth, placeable.height + top) {
                            placeable.placeRelative(start, top)
                        }
                    }) {
                        player.engine.FloatingLyricsCanvas(modifier = Modifier.fillMaxWidth(), config = config, textStyle = Theme.typography.v6.bold)
                    }
                }
            }
        }
    }
}