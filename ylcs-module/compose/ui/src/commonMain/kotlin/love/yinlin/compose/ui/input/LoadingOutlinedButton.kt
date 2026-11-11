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
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.animation.IndeterminateLoadingAnimation
import love.yinlin.concurrent.Mutex

@Composable
internal fun LoadingOutlinedButton(
    text: String,
    icon: ImageVector?,
    color: Color,
    style: TextStyle,
    animation: IndeterminateLoadingAnimation,
    padding: PaddingValues,
    enabled: Boolean,
    modifier: Modifier,
    onClick: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberFalse()
    val mutex = remember { Mutex() }

    OutlinedButton(onClick = {
        scope.onButtonLoading(mutex, onClick) { isLoading = it }
    }, color = color, enabled = enabled, padding = padding, modifier = modifier) {
        LoadingButtonAdapter(isLoading, text, icon, if (enabled) color else Theme.color.disabledContent, style, animation)
    }
}

@Composable
fun PrimaryLoadingOutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    LoadingOutlinedButton(text = text, icon = icon, color = Theme.color.primary, style = style, animation = animation, padding = padding, enabled = enabled, modifier = modifier, onClick = onClick)
}

@Composable
fun SecondaryLoadingOutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    LoadingOutlinedButton(text = text, icon = icon, color = Theme.color.secondary, style = style, animation = animation, padding = padding, enabled = enabled, modifier = modifier, onClick = onClick)
}

@Composable
fun TertiaryLoadingOutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    LoadingOutlinedButton(text = text, icon = icon, color = Theme.color.tertiary, style = style, animation = animation, padding = padding, enabled = enabled, modifier = modifier, onClick = onClick)
}
