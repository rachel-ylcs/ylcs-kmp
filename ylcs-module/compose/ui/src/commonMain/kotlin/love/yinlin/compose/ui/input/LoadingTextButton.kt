package love.yinlin.compose.ui.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.animation.IndeterminateLoadingAnimation
import love.yinlin.concurrent.Mutex

@Composable
fun LoadingTextButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = LocalColor.current,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberFalse()
    val mutex = remember { Mutex() }

    TextButton(onClick = {
        scope.onButtonLoading(mutex, onClick) { isLoading = it }
    }, enabled = enabled, padding = padding, modifier = modifier) {
        LoadingButtonAdapter(isLoading, text, icon, if (enabled) color else Theme.color.disabledContent, style, animation)
    }
}

@Composable
fun PrimaryLoadingTextButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    LoadingTextButton(text = text, icon = icon, color = Theme.color.primary, style = style, animation = animation, enabled = enabled, padding = padding, modifier = modifier, onClick = onClick)
}

@Composable
fun SecondaryLoadingTextButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    LoadingTextButton(text = text, icon = icon, color = Theme.color.secondary, style = style, animation = animation, enabled = enabled, padding = padding, modifier = modifier, onClick = onClick)
}

@Composable
fun TertiaryLoadingTextButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    LoadingTextButton(text = text, icon = icon, color = Theme.color.tertiary, style = style, animation = animation, enabled = enabled, padding = padding, modifier = modifier, onClick = onClick)
}