package love.yinlin.compose.ui.input

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter

@Composable
internal fun TextButton(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = Theme.shape.circle
    val minWidth = Theme.size.input5

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = minWidth, minHeight = minWidth / 3)
            .clip(shape)
            .pointerIcon(PointerIcon.Hand, enabled = enabled)
            .semantics(Role.Button)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(Theme.padding.value)
            .animateContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun TextButton(
    text: String,
    icon: ImageVector? = null,
    color: Color = LocalColor.current,
    style: TextStyle = LocalStyle.current.bold,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TextButton(enabled = enabled, modifier = modifier, onClick = onClick) {
        val contentColor = if (enabled) color else Theme.color.disabledContent
        TextIconAdapter { iconId, textId ->
            if (icon != null) Icon(icon = icon, color = contentColor, modifier = Modifier.iconId())
            Text(text = text, color = contentColor, style = style, modifier = Modifier.textId())
        }
    }
}

@Composable
fun PrimaryTextButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TextButton(text = text, icon = icon, color = Theme.color.primary, style = style, enabled = enabled, modifier = modifier, onClick = onClick)
}

@Composable
fun SecondaryTextButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TextButton(text = text, icon = icon, color = Theme.color.secondary, style = style, enabled = enabled, modifier = modifier, onClick = onClick)
}

@Composable
fun TertiaryTextButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TextButton(text = text, icon = icon, color = Theme.color.tertiary, style = style, enabled = enabled, modifier = modifier, onClick = onClick)
}