package love.yinlin.compose.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import love.yinlin.compose.*
import love.yinlin.compose.extension.localComposition
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.ColorIcon
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.HorizontalDivider
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text

internal val LocalSettingsThemeColor = localComposition<Color>()

@Stable
object SettingsScope {
    @Composable
    fun Item(
        title: String,
        icon: ImageVector? = null,
        hasDivider: Boolean = true,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = {},
        content: @Composable BoxScope.() -> Unit
    ) {
        Row(
            modifier = Modifier.heightIn(min = Theme.size.input8)
                .fillMaxWidth()
                .condition(onClick != null) { clickable(enabled = enabled, onClick = onClick) }
                .padding(Theme.padding.value9),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) ColorIcon(icon = icon, color = Theme.color.onContainer, background = LocalSettingsThemeColor.current)
            SimpleEllipsisText(text = title, style = Theme.typography.v7.bold)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd, content = content)
        }

        if (hasDivider) HorizontalDivider()
    }

    @Composable
    fun ItemText(
        title: String,
        icon: ImageVector? = null,
        text: String,
        maxLines: Int = 1,
        hasDivider: Boolean = true,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = {},
    ) {
        Item(
            title = title,
            icon = icon,
            hasDivider = hasDivider,
            enabled = enabled,
            onClick = onClick
        ) {
            Text(
                text = text,
                color = LocalColorVariant.current,
                style = Theme.typography.v8,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun ItemSwitch(
        title: String,
        icon: ImageVector? = null,
        enabled: Boolean = true,
        hasDivider: Boolean = true,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit = {}
    ) {
        Item(
            title = title,
            icon = icon,
            hasDivider = hasDivider,
            enabled = enabled,
            onClick = null
        ) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }

    @Composable
    fun ItemExpander(
        title: String,
        icon: ImageVector? = null,
        text: String? = null,
        hasDivider: Boolean = true,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = {},
    ) {
        Item(
            title = title,
            icon = icon,
            hasDivider = hasDivider,
            enabled = enabled,
            onClick = onClick
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (text != null) {
                    SimpleEllipsisText(
                        text = text,
                        color = LocalColorVariant.current,
                        style = Theme.typography.v8
                    )
                }
                Icon(icon = Icons.KeyboardArrowRight)
            }
        }
    }

    @Suppress("AssignedValueIsNeverRead")
    @Composable
    fun ItemExpanderSuspend(
        title: String,
        icon: ImageVector? = null,
        text: String? = null,
        hasDivider: Boolean = true,
        enabled: Boolean = true,
        onClick: suspend () -> Unit = {}
    ) {
        val scope = rememberCoroutineScope()
        var isLoading by rememberFalse()

        Item(
            title = title,
            icon = icon,
            hasDivider = hasDivider,
            enabled = enabled && !isLoading,
            onClick = {
                scope.launch {
                    isLoading = true
                    onClick()
                    isLoading = false
                }
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (text != null) {
                    SimpleEllipsisText(
                        text = text,
                        color = LocalColorVariant.current,
                        style = Theme.typography.v8
                    )
                }
                if (isLoading) CircleLoading.Content()
                else Icon(icon = Icons.KeyboardArrowRight)
            }
        }
    }
}

@Composable
fun SettingsLayout(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable SettingsScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = Theme.shape.v3,
        shadowElevation = Theme.shadow.v3
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ThemeContainer(color) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon = icon)
                    SimpleEllipsisText(text = title, style = Theme.typography.v6.bold)
                }
            }
            CompositionLocalProvider(LocalSettingsThemeColor provides color.copy(alpha = 0.75f)) {
                SettingsScope.content()
            }
        }
    }
}