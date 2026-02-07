package love.yinlin.compose.ui.input

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.node.shadow
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter

@Composable
@NonRestartableComposable
internal fun Button(
    onClick: () -> Unit,
    color: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val contentColor = if (enabled) Theme.color.onContainer else Theme.color.disabledContent
    val contentColorVariant = if (enabled) Theme.color.onContainerVariant else Theme.color.disabledContent
    val backgroundColor = if (enabled) color else Theme.color.disabledContainer

    CompositionLocalProvider(
        LocalColor provides contentColor,
        LocalColorVariant provides contentColorVariant,
    ) {
        val shape = Theme.shape.circle
        val minWidth = Theme.size.input5
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = minWidth, minHeight = minWidth / 3)
                .shadow(shape, Theme.shadow.v6)
                .clip(shape)
                .background(color = backgroundColor)
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
}

@Composable
private fun Button(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector?,
    color: Color,
    style: TextStyle = LocalStyle.current.bold,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(onClick = onClick, color = color, enabled = enabled, modifier = modifier) {
        TextIconAdapter{ iconId, textId ->
            if (icon != null) Icon(icon = icon, modifier = Modifier.iconId())
            Text(text = text, style = style, modifier = Modifier.textId())
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(onClick = onClick, text = text, icon = icon, color = Theme.color.primaryContainer, style = style, enabled = enabled, modifier = modifier)
}

@Composable
fun SecondaryButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(onClick = onClick, text = text, icon = icon, color = Theme.color.secondaryContainer, style = style, enabled = enabled, modifier = modifier)
}

@Composable
fun TertiaryButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(onClick = onClick, text = text, icon = icon, color = Theme.color.tertiaryContainer, style = style, enabled = enabled, modifier = modifier)
}