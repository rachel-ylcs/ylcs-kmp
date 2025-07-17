package love.yinlin.ui.component.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.platform.app

@Composable
fun <S> AnimationLayout(
    state: S,
    duration: Int = app.config.animationSpeed,
    modifier: Modifier = Modifier,
    content: @Composable (AnimatedContentScope.(S) -> Unit)
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (fadeIn(animationSpec = tween(duration, delayMillis = duration / 2)) +
                    scaleIn(initialScale = 0.9f, animationSpec = tween(duration, delayMillis = duration / 2)))
                .togetherWith(fadeOut(animationSpec = tween(duration / 2)))
        },
        modifier = modifier,
        content = content
    )
}