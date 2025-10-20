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
import kotlinx.coroutines.delay
import love.yinlin.common.Colors
import love.yinlin.common.ExtraIcons
import love.yinlin.common.ThemeValue
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.rememberFalse
import love.yinlin.ui.component.node.clickableNoRipple
import love.yinlin.extension.timeString
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.input.ProgressSlider
import love.yinlin.ui.component.layout.Space

@Composable
expect fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
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
            animationSpec = tween(durationMillis = app.config.animationSpeed, easing = LinearOutSlowInEasing),
            initialOffsetY = animateOffset
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = app.config.animationSpeed, easing = LinearOutSlowInEasing),
            targetOffsetY = animateOffset
        )
    ) {
        Row(
            modifier = modifier.background(Colors.Dark.copy(alpha = 0.5f)).padding(ThemeValue.Padding.Value),
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
        var isShowControls by rememberFalse()

        LaunchedEffect(Unit) {
            delay(app.config.animationSpeed.toLong())
            isShowControls = true
        }

        if (topBar != null) {
            VideoPlayerControlBar(
                visible = isShowControls,
                animateOffset = { -it },
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxWidth(),
                content = topBar
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f).clickableNoRipple {
            isShowControls = !isShowControls
        })

        VideoPlayerControlBar(
            visible = isShowControls,
            animateOffset = { it },
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxWidth()
        ) {
            ClickIcon(
                icon = if (isPlaying) ExtraIcons.Pause else ExtraIcons.Play,
                color = Colors.White,
                onClick = onPlayClick
            )
            Space()
            if (leftAction != null) {
                leftAction()
                Space()
            }
            ProgressSlider(
                value = if (duration == 0L) 0f else position / duration.toFloat(),
                onValueChangeFinished = { onProgressClick((it * duration).toLong()) },
                modifier = Modifier.weight(1f)
            )
            Space()
            Text(
                text = remember(position) { position.timeString },
                color = Colors.White
            )
            Space(ThemeValue.Padding.HorizontalSpace / 2)
            Text(
                text = "/",
                color = Colors.White
            )
            Space(ThemeValue.Padding.HorizontalSpace / 2)
            Text(
                text = remember(duration) { duration.timeString },
                color = Colors.White
            )
            if (rightAction != null) {
                Space()
                rightAction()
            }
        }
    }
}