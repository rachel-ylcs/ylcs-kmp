package love.yinlin.ui.component.platform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import love.yinlin.Local
import love.yinlin.common.Colors
import love.yinlin.common.ExtraIcons
import love.yinlin.extension.clickableNoRipple
import love.yinlin.extension.rememberState
import love.yinlin.extension.timeString
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.input.BeautifulSlider
import love.yinlin.ui.component.layout.Space

@Composable
expect fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    topBar: (@Composable RowScope.() -> Unit)? = null
)

@Composable
private fun VideoPlayerControlBar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    animateOffset: (Int) -> Int,
    content: @Composable RowScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = Local.Client.ANIMATION_DURATION, easing = LinearOutSlowInEasing),
            initialOffsetY = animateOffset
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = Local.Client.ANIMATION_DURATION, easing = LinearOutSlowInEasing),
            targetOffsetY = animateOffset
        )
    ) {
        Row(
            modifier = modifier.background(Colors.Dark.copy(alpha = 0.5f)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun VideoPlayerControls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    position: Long,
    duration: Long,
    onProgressClick: (Long) -> Unit,
    topBar: (@Composable RowScope.() -> Unit)? = null,
    leftAction: (@Composable RowScope.() -> Unit)? = null,
    rightAction: (@Composable RowScope.() -> Unit)? = null
) {
    Column(modifier = modifier.clipToBounds()) {
        var isShowControls by rememberState { false }

        LaunchedEffect(Unit) {
            delay(500)
            isShowControls = true
        }

        if (topBar != null) {
            VideoPlayerControlBar(
                visible = isShowControls,
                animateOffset = { -it },
                modifier = Modifier.fillMaxWidth(),
                content = topBar
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f).clickableNoRipple {
            isShowControls = !isShowControls
        })

        VideoPlayerControlBar(
            visible = isShowControls,
            animateOffset = { it },
            modifier = Modifier.fillMaxWidth()
        ) {
            ClickIcon(
                icon = if (isPlaying) ExtraIcons.Pause else ExtraIcons.Play,
                color = Colors.White,
                onClick = onPlayClick
            )
            Space(10.dp)
            if (leftAction != null) {
                leftAction()
                Space(10.dp)
            }
            BeautifulSlider(
                value = if (duration == 0L) 0f else position / duration.toFloat(),
                onValueChangeFinished = { onProgressClick((it * duration).toLong()) },
                modifier = Modifier.weight(1f)
            )
            Space(10.dp)
            Text(
                text = remember(position) { position.timeString },
                color = Colors.White
            )
            Space(5.dp)
            Text(
                text = "/",
                color = Colors.White
            )
            Space(5.dp)
            Text(
                text = remember(duration) { duration.timeString },
                color = Colors.White
            )
            if (rightAction != null) {
                Space(10.dp)
                rightAction()
            }
        }
    }
}