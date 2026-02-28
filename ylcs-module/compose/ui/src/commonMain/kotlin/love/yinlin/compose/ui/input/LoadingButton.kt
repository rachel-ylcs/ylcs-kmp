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
private fun LoadingButton(
    text: String,
    icon: ImageVector?,
    color: Color,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberFalse()
    val mutex = remember { Mutex() }

    Button(onClick = {
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
    }, color = color, enabled = enabled, modifier = modifier) {
        TextIconAdapter { iconId, textId ->
            if (isLoading) animation.Content(modifier = Modifier.iconId())
            else if (icon != null) Icon(icon = icon, modifier = Modifier.iconId())
            Text(text = text, style = style, modifier = Modifier.textId())
        }
    }
}

@Composable
fun PrimaryLoadingButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit
) {
    LoadingButton(onClick = onClick, text = text, icon = icon, color = Theme.color.primaryContainer, style = style, animation = animation, enabled = enabled, modifier = modifier)
}

@Composable
fun SecondaryLoadingButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit
) {
    LoadingButton(onClick = onClick, text = text, icon = icon, color = Theme.color.secondaryContainer, style = style, animation = animation, enabled = enabled, modifier = modifier)
}

@Composable
fun TertiaryLoadingButton(
    text: String,
    icon: ImageVector? = null,
    style: TextStyle = LocalStyle.current.bold,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit
) {
    LoadingButton(onClick = onClick, text = text, icon = icon, color = Theme.color.tertiaryContainer, style = style, animation = animation, enabled = enabled, modifier = modifier)
}