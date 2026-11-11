package love.yinlin.compose.ui.input

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.animation.IndeterminateLoadingAnimation
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.node.shadow
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.concurrent.Mutex
import love.yinlin.extension.catching
import love.yinlin.extension.cleaning

internal const val DEFAULT_BUTTON_HEIGHT_ASPECT_RATIO = 2.5f

internal inline fun CoroutineScope.onButtonLoading(mutex: Mutex, crossinline onClick: suspend () -> Unit, crossinline onLoading: (Boolean) -> Unit) {
    this.launch {
        if (!mutex.tryLock()) return@launch
        onLoading(true)
        cleaning({
            catching { onClick() }
        }) {
            onLoading(false)
            mutex.unlock()
        }
    }
}

@Composable
internal fun LoadingButtonAdapter(
    isLoading: Boolean,
    text: String,
    icon: ImageVector?,
    color: Color,
    style: TextStyle,
    animation: IndeterminateLoadingAnimation
) {
    TextIconAdapter { iconId, textId ->
        if (isLoading) animation.Content(color = color, modifier = Modifier.iconId())
        else if (icon != null) Icon(icon = icon, color = color, modifier = Modifier.iconId())
        Text(text = text, color = color, style = style, modifier = Modifier.textId())
    }
}

@Composable
@NonRestartableComposable
fun Button(
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    ThemeContainer(enabled) {
        val shape = Theme.shape.circle
        val minWidth = Theme.size.input5
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = minWidth, minHeight = minWidth / DEFAULT_BUTTON_HEIGHT_ASPECT_RATIO)
                .shadow(shape, Theme.shadow.v6)
                .clip(shape)
                .background(color = if (enabled) color else Theme.color.disabledContainer)
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
}

@Composable
fun Button(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector?,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalStyle.current.bold,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true
) {
    Button(onClick = onClick, color = color, padding = padding, enabled = enabled, modifier = modifier) {
        TextIconAdapter { iconId, textId ->
            if (icon != null) Icon(icon = icon, modifier = Modifier.iconId())
            Text(text = text, style = style, modifier = Modifier.textId())
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(onClick = onClick, text = text, icon = icon, color = Theme.color.primaryContainer, style = style, padding = padding, enabled = enabled, modifier = modifier)
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(onClick = onClick, text = text, icon = icon, color = Theme.color.secondaryContainer, style = style, padding = padding, enabled = enabled, modifier = modifier)
}

@Composable
fun TertiaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(onClick = onClick, text = text, icon = icon, color = Theme.color.tertiaryContainer, style = style, padding = padding, enabled = enabled, modifier = modifier)
}