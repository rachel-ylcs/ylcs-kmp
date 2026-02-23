package love.yinlin.compose.ui.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.extension.timeString

@Stable
interface VideoActionBar {
    @Composable
    fun RowScope.Content(controller: VideoController)

    companion object {
        fun topDefault(onBack: () -> Unit) = object : VideoActionBar {
            @Composable
            override fun RowScope.Content(controller: VideoController) {
                Icon(icon = Icons.ArrowBack, onClick = onBack)
                Box(modifier = Modifier.weight(1f))
                Icon(icon = Icons.FullScreen, onClick = { controller.orientationController.rotate() })
            }
        }

        val Progress = object : VideoActionBar {
            @Composable
            override fun RowScope.Content(controller: VideoController) {
                val progress by rememberDerivedState {
                    val duration = controller.duration
                    if (duration == 0L) 0f else controller.position / duration.toFloat()
                }
                val currentTime by rememberDerivedState { controller.position.timeString }
                val durationTime by rememberDerivedState { controller.duration.timeString }

                Icon(icon = if (controller.isPlaying) Icons.Pause else Icons.Play, onClick = {
                    if (controller.isPlaying) controller.pause()
                    else controller.play()
                })

                Slider(
                    value = progress,
                    onValueChangeFinished = { controller.seek((it * controller.duration).toLong()) },
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Theme.padding.h / 2)) {
                    SimpleEllipsisText(text = currentTime)
                    SimpleEllipsisText(text = "/")
                    SimpleEllipsisText(text = durationTime)
                }
            }
        }
    }
}