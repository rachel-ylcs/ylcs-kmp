package love.yinlin.compose.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme

@Composable
fun BackgroundContainer(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColor provides Theme.color.onBackground,
        LocalColorVariant provides Theme.color.onBackgroundVariant,
        content = content
    )
}

@Composable
fun SurfaceContainer(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColor provides Theme.color.onSurface,
        LocalColorVariant provides Theme.color.onSurfaceVariant,
        content = content
    )
}

@Composable
fun ThemeContainer(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColor provides Theme.color.onContainer,
        LocalColorVariant provides Theme.color.onContainerVariant,
        content = content
    )
}

@Composable
fun ThemeContainer(enabled: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColor provides if (enabled) Theme.color.onContainer else Theme.color.disabledContent,
        LocalColorVariant provides if (enabled) Theme.color.onContainerVariant else Theme.color.disabledContent,
        content = content
    )
}

@Composable
fun ThemeContainer(color: Color, variantColor: Color = color.copy(alpha = 0.75f), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColor provides color,
        LocalColorVariant provides variantColor,
        content = content
    )
}

@Composable
fun ThemeContainer(enabled: Boolean, color: Color, variantColor: Color = color.copy(alpha = 0.75f), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColor provides if (enabled) color else Theme.color.disabledContent,
        LocalColorVariant provides if (enabled) variantColor else Theme.color.disabledContent,
        content = content
    )
}