package love.yinlin.compose.ui.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme

@Composable
fun <S> AnimationContent(
    state: S,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopStart,
    contentKey: (S) -> Any? = { it },
    duration: Int? = null,
    enter: ((Int) -> EnterTransition)? = null,
    exit: ((Int) -> ExitTransition)? = null,
    content: @Composable (AnimatedContentScope.(S) -> Unit)
) {
    val animationTheme = Theme.animation
    val animationDuration = duration ?: animationTheme.duration.default

    AnimatedContent(
        targetState = state,
        modifier = modifier,
        contentAlignment = alignment,
        transitionSpec = {
            val enterAnimation = enter ?: animationTheme.enter
            val exitAnimation = exit ?: animationTheme.exit
            enterAnimation(animationDuration) togetherWith exitAnimation(animationDuration)
        },
        contentKey = contentKey,
        content = content,
    )
}

@Composable
fun AnimationVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    duration: Int? = null,
    enter: ((Int) -> EnterTransition)? = null,
    exit: ((Int) -> ExitTransition)? = null,
    content: @Composable (AnimatedVisibilityScope.() -> Unit)
) {
    val animationTheme = Theme.animation
    val animationDuration = duration ?: animationTheme.duration.default
    val enterAnimation = enter ?: animationTheme.enter
    val exitAnimation = exit ?: animationTheme.exit

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enterAnimation(animationDuration),
        exit = exitAnimation(animationDuration),
        content = content
    )
}