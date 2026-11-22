package love.yinlin.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.LocalNativeWindowContext
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.platform.Platform
import love.yinlin.platform.platform

@Composable
fun DefaultTopBarActions(controller: WindowController, onExit: () -> Unit) {
    Icon(
        icon = Icons.Remove,
        tip = Theme.value.windowMinimizeText,
        onClick = { controller.minimize = true },
        modifier = Modifier.size(Theme.size.smallIcon)
    )
    val canMaximize = controller.resizable || controller.maximize
    Icon(
        icon = Icons.CropSquare,
        tip = if (controller.maximize) Theme.value.windowMaximizeBackText else Theme.value.windowMaximizeText,
        onClick = if (canMaximize) controller::toggleMaximize else null,
        modifier = Modifier.size(Theme.size.smallIcon).maximizeButtonBounds(canMaximize)
    )
    Icon(
        icon = Icons.Clear,
        tip = Theme.value.windowCloseText,
        onClick = onExit,
        modifier = Modifier.size(Theme.size.smallIcon)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Modifier.maximizeButtonBounds(enabled: Boolean): Modifier {
    if (!enabled || platform != Platform.WindowsNative) return this
    val window = LocalNativeWindowContext.current.window

    DisposableEffect(window) {
        onDispose { window.setMaximizeButtonBounds(0f, 0f, 0f, 0f) }
    }

    return onGloballyPositioned {
        (val left, val top, val right, val bottom) = it.boundsInWindow()
        window.setMaximizeButtonBounds(left, top, right, bottom)
    }
}

@Composable
fun DefaultTopBar(
    controller: WindowController,
    onExit: () -> Unit,
    backgroundColor: Color = Colors.Transparent,
    actions: @Composable RowScope.() -> Unit = { DefaultTopBarActions(controller, onExit) },
) {
    val bgBrush = remember(backgroundColor) {
        if (backgroundColor == Colors.Transparent) SolidColor(backgroundColor)
        else Brush.verticalGradient(colors = [backgroundColor.copy(alpha = 0.75f), backgroundColor])
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(bgBrush).padding(Theme.padding.value9),
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = controller.iconPainter,
                color = Colors.Unspecified,
                modifier = Modifier.size(Theme.size.smallIcon)
            )
            SimpleEllipsisText(text = controller.title, style = Theme.typography.v7.bold)
        }
        ActionScope.Right.Container(content = actions)
    }
}
