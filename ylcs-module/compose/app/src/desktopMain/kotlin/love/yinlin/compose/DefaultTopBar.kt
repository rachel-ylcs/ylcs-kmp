package love.yinlin.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleEllipsisText

@Composable
fun DefaultTopBarActions(controller: WindowController, onExit: () -> Unit) {
    Icon(
        icon = Icons.Remove,
        tip = Theme.value.windowMinimizeText,
        onClick = { controller.minimize = true }
    )
    Icon(
        icon = Icons.CropSquare,
        tip = if (controller.maximize) Theme.value.windowMaximizeBackText else Theme.value.windowMaximizeText,
        onClick = { controller.toggleMaximize() }
    )
    Icon(
        icon = Icons.Clear,
        tip = Theme.value.windowCloseText,
        onClick = onExit
    )
}

@Composable
fun DefaultTopBar(
    controller: WindowController,
    onExit: () -> Unit,
    actions: @Composable RowScope.() -> Unit = { DefaultTopBarActions(controller, onExit) },
) {
    val backgroundColor = Theme.color.primaryContainer
    val bgBrush = remember(backgroundColor) {
        Brush.verticalGradient(colors = listOf(backgroundColor.copy(alpha = 0.75f), backgroundColor))
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(bgBrush).padding(Theme.padding.value9),
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeContainer {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = controller.iconPainter,
                    color = controller.icon?.let { Colors.Unspecified } ?: LocalColor.current,
                    modifier = Modifier.size(Theme.size.icon)
                )

                SimpleEllipsisText(
                    text = controller.title,
                    style = Theme.typography.v6.bold
                )
            }

            ActionScope.Right.Container(modifier = Modifier.weight(1f), content = actions)
        }
    }
}