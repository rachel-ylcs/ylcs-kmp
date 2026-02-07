package love.yinlin.compose.ui.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.launch
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.animation.IndeterminateLoadingAnimation
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.concurrent.Mutex
import love.yinlin.extension.catching

@Composable
fun LoadingTextButton(
    text: String,
    icon: ImageVector? = null,
    color: Color = LocalColor.current,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberFalse()
    val mutex = remember { Mutex() }

    TextButton(onClick = {
        if (!isLoading) {
            scope.launch {
                if (!isLoading) {
                    mutex.with {
                        if (!isLoading) {
                            isLoading = true
                            catching { onClick() }
                            isLoading = false
                        }
                    }
                }
            }
        }
    }, enabled = enabled, modifier = modifier) {
        val contentColor = if (enabled) color else Theme.color.disabledContent
        TextIconAdapter { iconId, textId ->
            if (isLoading) animation.Content(color = contentColor, modifier = Modifier.iconId())
            else if (icon != null) Icon(icon = icon, color = contentColor, modifier = Modifier.iconId())
            Text(text = text, color = contentColor, style = style, modifier = Modifier.textId())
        }
    }
}

@Composable
fun PrimaryLoadingTextButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit
) {
    LoadingTextButton(text = text, icon = icon, color = Theme.color.primary, style = style, animation = animation, enabled = enabled, modifier = modifier, onClick = onClick)
}

@Composable
fun SecondaryLoadingTextButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit
) {
    LoadingTextButton(text = text, icon = icon, color = Theme.color.secondary, style = style, animation = animation, enabled = enabled, modifier = modifier, onClick = onClick)
}

@Composable
fun TertiaryLoadingTextButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit
) {
    LoadingTextButton(text = text, icon = icon, color = Theme.color.tertiary, style = style, animation = animation, enabled = enabled, modifier = modifier, onClick = onClick)
}