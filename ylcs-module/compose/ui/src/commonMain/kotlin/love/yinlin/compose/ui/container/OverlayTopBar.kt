package love.yinlin.compose.ui.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.Button
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.concurrent.Mutex
import love.yinlin.extension.catching

@Stable
sealed interface OverlayAction {
    val text: String

    val icon: ImageVector

    val enabled: Boolean

    @Composable
    fun Content(ltr: Boolean)

    @Stable
    data class Sync(override val text: String, override val icon: ImageVector, override val enabled: Boolean = true, val onClick: () -> Unit) : OverlayAction {
        @Composable
        override fun Content(ltr: Boolean) {
            Button(
                color = if (ltr) Theme.color.secondaryContainer else Theme.color.tertiaryContainer,
                padding = Theme.padding.value10,
                enabled = enabled,
                onClick = onClick
            ) {
                TextIconAdapter(ltr = ltr) { iconId, textId ->
                    Icon(icon = icon, modifier = Modifier.iconId())
                    Text(text = text, style = Theme.typography.v6.bold, modifier = Modifier.textId())
                }
            }
        }
    }

    @Stable
    data class Async(override val text: String, override val icon: ImageVector, override val enabled: Boolean = true, val onClick: suspend () -> Unit) : OverlayAction {
        @Composable
        override fun Content(ltr: Boolean) {
            val scope = rememberCoroutineScope()
            var isLoading by rememberFalse()
            val mutex = remember { Mutex() }

            Button(
                color = if (ltr) Theme.color.secondaryContainer else Theme.color.tertiaryContainer,
                padding = Theme.padding.value10,
                enabled = enabled,
                onClick = {
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
                }
            ) {
                TextIconAdapter(ltr = ltr) { iconId, textId ->
                    if (isLoading) CircleLoading.Content(modifier = Modifier.iconId())
                    else Icon(icon = icon, modifier = Modifier.iconId())
                    Text(text = text, style = Theme.typography.v6.bold, modifier = Modifier.textId())
                }
            }
        }
    }
}

@Composable
fun OverlayTopBar(
    modifier: Modifier = Modifier,
    left: OverlayAction? = null,
    right: OverlayAction? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        left?.Content(true)
        right?.Content(false)
    }
}