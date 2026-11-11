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
private fun LoadingButton(
    text: String,
    icon: ImageVector?,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberFalse()
    val mutex = remember { Mutex() }

    Button(onClick = {
        scope.onButtonLoading(mutex, onClick) { isLoading = it }
    }, color = color, padding = padding, enabled = enabled, modifier = modifier) {
        LoadingButtonAdapter(isLoading, text, icon, LocalColor.current, style, animation)
    }
}

@Composable
fun PrimaryLoadingButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    LoadingButton(onClick = onClick, text = text, icon = icon, color = Theme.color.primaryContainer, style = style, animation = animation, padding = padding, enabled = enabled, modifier = modifier)
}

@Composable
fun SecondaryLoadingButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    LoadingButton(onClick = onClick, text = text, icon = icon, color = Theme.color.secondaryContainer, style = style, animation = animation, padding = padding, enabled = enabled, modifier = modifier)
}

@Composable
fun TertiaryLoadingButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    padding: PaddingValues = Theme.padding.value10,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    LoadingButton(onClick = onClick, text = text, icon = icon, color = Theme.color.tertiaryContainer, style = style, animation = animation, padding = padding, enabled = enabled, modifier = modifier)
}