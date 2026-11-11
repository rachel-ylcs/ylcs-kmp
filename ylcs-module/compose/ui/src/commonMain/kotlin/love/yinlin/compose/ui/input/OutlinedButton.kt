package love.yinlin.compose.ui.input

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter

@Composable
internal fun OutlinedButton(
    modifier: Modifier,
    color: Color,
    padding: PaddingValues,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = Theme.shape.circle
    val minWidth = Theme.size.input5
    val contentColor = if (enabled) color else Theme.color.disabledContent

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = minWidth, minHeight = minWidth / DEFAULT_BUTTON_HEIGHT_ASPECT_RATIO)
            .clip(shape)
            .border(Theme.border.v6, contentColor, shape)
            .pointerIcon(PointerIcon.Hand, enabled = enabled)
            .semantics(Role.Button)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(padding)
            .animateContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun OutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = Theme.color.outline,
    style: TextStyle = LocalStyle.current.bold,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(color = color, padding = padding, enabled = enabled, modifier = modifier, onClick = onClick) {
        val contentColor = if (enabled) color else Theme.color.disabledContent
        TextIconAdapter { iconId, textId ->
            if (icon != null) Icon(icon = icon, color = contentColor, modifier = Modifier.iconId())
            Text(text = text, color = contentColor, style = style, modifier = Modifier.textId())
        }
    }
}

@Composable
fun PrimaryOutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(text = text, icon = icon, color = Theme.color.primary, style = style, padding = padding, enabled = enabled, modifier = modifier, onClick = onClick)
}

@Composable
fun SecondaryOutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(text = text, icon = icon, color = Theme.color.secondary, style = style, padding = padding, enabled = enabled, modifier = modifier, onClick = onClick)
}

@Composable
fun TertiaryOutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(text = text, icon = icon, color = Theme.color.tertiary, style = style, padding = padding, enabled = enabled, modifier = modifier, onClick = onClick)
}
