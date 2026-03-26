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
import kotlinx.coroutines.delay
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.animation.AnimationVisibility
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.node.silentClick
import kotlin.time.Duration.Companion.seconds

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

@Stable
abstract class VideoController(topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoState() {
    protected abstract fun releaseController()

    @Composable
    abstract fun SurfaceContent(modifier: Modifier = Modifier)

    private val videoTopBar = topBar(this)
    private val videoBottomBar = bottomBar(this)
    var isShowControls by mutableStateOf(false)
        private set

    @Composable
    fun PlayerControls(modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            LaunchedEffect(isShowControls) {
                if (isShowControls) {
                    delay(5.seconds)
                    isShowControls = false
                }
            }

            DisposableEffect(Unit) {
                isShowControls = true
                onDispose {
                    isShowControls = false
                }
            }

            ThemeContainer(Colors.White) {
                videoTopBar?.let { bar ->
                    VideoPlayerControlBar(
                        visible = isShowControls,
                        animateOffset = { -it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        with(bar) { Content() }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f).silentClick { isShowControls = !isShowControls })

                videoBottomBar?.let { bar ->
                    VideoPlayerControlBar(
                        visible = isShowControls,
                        animateOffset = { it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        with(bar) { Content() }
                    }
                }
            }
        }
    }

    fun release() {
        videoTopBar?.release()
        videoBottomBar?.release()
        releaseController()
    }
}