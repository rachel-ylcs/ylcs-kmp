package love.yinlin.compose.ui.input

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter

@Composable
internal fun OutlinedButton(
    color: Color,
    enabled: Boolean,
    modifier: Modifier,
    loading: Boolean = false,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit
) {
    val contentColor = if (enabled) color else Theme.color.disabledContent
    TextButton(
        enabled = enabled && !loading,
        modifier = modifier.border(Theme.border.v7, contentColor, Theme.shape.circle),
        onClick = onClick
    ) {
        content(contentColor)
    }
}

@Composable
internal fun OutlinedButton(
    text: String,
    icon: ImageVector?,
    color: Color,
    style: TextStyle,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(color = color, enabled = enabled, modifier = modifier, onClick = onClick) { contentColor ->
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(text = text, icon = icon, color = Theme.color.primary, style = style, enabled = enabled, modifier = modifier, onClick = onClick)
}

@Composable
fun SecondaryOutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(text = text, icon = icon, color = Theme.color.secondary, style = style, enabled = enabled, modifier = modifier, onClick = onClick)
}

@Composable
fun TertiaryOutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(text = text, icon = icon, color = Theme.color.tertiary, style = style, enabled = enabled, modifier = modifier, onClick = onClick)
}
