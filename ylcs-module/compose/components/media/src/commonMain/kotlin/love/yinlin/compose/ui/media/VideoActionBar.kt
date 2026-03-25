package love.yinlin.compose.ui.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.window.rememberOrientationController
import love.yinlin.extension.timeString

@Stable
interface VideoActionBar {
    typealias Factory = (VideoState) -> VideoActionBar?

    @Composable
    fun RowScope.Content()

    @Stable
    private class ProgressActionBar(private val state: VideoState) : VideoActionBar {
        val progress by derivedStateOf {
            val duration = state.duration
            if (duration == 0L) 0f else state.position / duration.toFloat()
        }

        @Composable
        override fun RowScope.Content() {
            Icon(icon = if (state.isPlaying) Icons.Pause else Icons.Play, onClick = {
                if (state.isPlaying) state.pause()
                else state.play()
            })

            Slider(
                value = progress,
                onValueChangeFinished = { state.seek((it * state.duration).toLong()) },
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Theme.padding.h / 2)) {
                SimpleEllipsisText(text = state.position.timeString)
                SimpleEllipsisText(text = "/")
                SimpleEllipsisText(text = state.duration.timeString)
            }
        }
    }

    @Stable
    private class TopDefaultActionBar(private val onBack: () -> Unit) : VideoActionBar {
        @Composable
        override fun RowScope.Content() {
            val orientationController = rememberOrientationController()

            Icon(icon = Icons.ArrowBack, onClick = onBack)
            Box(modifier = Modifier.weight(1f))
            Icon(icon = Icons.FullScreen, onClick = orientationController::rotate)
        }
    }

    companion object {
        val None: Factory = { null }
        val Progress: Factory = ::ProgressActionBar
        fun topDefault(onBack: () -> Unit): Factory = { TopDefaultActionBar(onBack) }
    }
}