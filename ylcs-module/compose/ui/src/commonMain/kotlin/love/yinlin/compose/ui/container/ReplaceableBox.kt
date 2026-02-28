package love.yinlin.compose.ui.container

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon

@Composable
fun <T> ReplaceableBox(
    value: T? = null,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    shape: Shape = Theme.shape.v7,
    content: @Composable (T) -> Unit
) {
    val minSize = Theme.size.input6

    AnimationContent(
        state = value,
        enter = { fadeIn(animationSpec = tween(it, it / 2)) },
        exit = { fadeOut(animationSpec = tween(it, it / 2)) },
        modifier = Modifier.animateContentSize()
    ) { targetValue ->
        Box(
            modifier = Modifier.defaultMinSize(minWidth = minSize, minHeight = minSize),
            propagateMinConstraints = true
        ) {
            if (targetValue == null) {
                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(Theme.color.backgroundVariant)
                        .clickable(onClick = onReplace),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon = Icons.Add, color = Theme.color.onBackground)
                }
            }
            else {
                Box(
                    modifier = Modifier.clip(shape).combinedClickable(onClick = onReplace, onLongClick = onDelete),
                    propagateMinConstraints = true
                ) {
                    content(targetValue)
                }
            }
        }
    }
}