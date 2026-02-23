package love.yinlin.compose.ui.media

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import kotlinx.coroutines.delay
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.AnimationVisibility
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.foundation.Context
import love.yinlin.foundation.OrientationController

@Stable
abstract class VideoState(context: Context, val topBar: VideoActionBar?, val bottomBar: VideoActionBar?) {
    var url: String? by mutableStateOf(null)
        protected set

    var isPlaying by mutableStateOf(false)
        protected set

    var position: Long by mutableLongStateOf(0L)
        protected set

    var duration by mutableLongStateOf(0L)
        protected set

    var error: Throwable? by mutableStateOf(null)
        protected set

    val orientationController = OrientationController(context)

    abstract fun release()
    abstract fun load(path: String)
    abstract fun play()
    abstract fun pause()
    abstract fun stop()
    abstract fun seek(position: Long)

    @Composable
    private fun VideoPlayerControlBar(
        visible: Boolean,
        modifier: Modifier = Modifier,
        animateOffset: (Int) -> Int,
        content: @Composable RowScope.() -> Unit
    ) {
        AnimationVisibility(
            visible = visible,
            modifier = modifier,
            enter = {
                slideInVertically(
                    animationSpec = tween(durationMillis = it, easing = LinearOutSlowInEasing),
                    initialOffsetY = animateOffset
                )
            },
            exit = {
                slideOutVertically(
                    animationSpec = tween(durationMillis = it, easing = LinearOutSlowInEasing),
                    targetOffsetY = animateOffset
                )
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Colors.Dark.copy(alpha = 0.5f)).padding(Theme.padding.value),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }

    @Composable
    fun VideoPlayerControls(modifier: Modifier = Modifier) {
        Column(modifier = modifier.clipToBounds()) {
            var isShowControls by rememberFalse()

            LaunchedEffect(Unit) {
                isShowControls = true
            }

            LaunchedEffect(isShowControls) {
                if (isShowControls) {
                    delay(5000L)
                    isShowControls = false
                }
            }

            ThemeContainer(Colors.White) {
                val controller = this@VideoState as VideoController
                topBar?.apply {
                    VideoPlayerControlBar(
                        visible = isShowControls,
                        animateOffset = { -it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Content(controller)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f).silentClick { isShowControls = !isShowControls })

                bottomBar?.apply {
                    VideoPlayerControlBar(
                        visible = isShowControls,
                        animateOffset = { it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Content(controller)
                    }
                }
            }
        }
    }
}