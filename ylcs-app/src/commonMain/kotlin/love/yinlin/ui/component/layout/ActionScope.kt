package love.yinlin.ui.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.CoroutineScope
import love.yinlin.common.ThemeValue
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.LoadingIcon

@Stable
sealed class ActionScope(private val ltr: Boolean) {
	@Stable
	object Left : ActionScope(true)
	@Stable
	object Right : ActionScope(false)

	@Composable
	fun Action(
        icon: ImageVector,
        color: Color = MaterialTheme.colorScheme.onSurface,
        enabled: Boolean = true,
        onClick: () -> Unit
	) {
		val padding = if (ltr) ThemeValue.Padding.HorizontalSpace else ThemeValue.Padding.ZeroSpace

        ClickIcon(
            icon = icon,
            color = color,
            enabled = enabled,
            modifier = Modifier.padding(start = padding, end = ThemeValue.Padding.HorizontalSpace - padding),
            onClick = onClick
        )
	}

	@Composable
	fun ActionSuspend(
        icon: ImageVector,
        color: Color = MaterialTheme.colorScheme.onSurface,
        enabled: Boolean = true,
        onClick: suspend CoroutineScope.() -> Unit
	) {
		val padding = if (ltr) ThemeValue.Padding.HorizontalSpace else ThemeValue.Padding.ZeroSpace

        LoadingIcon(
            icon = icon,
            color = color,
            enabled = enabled,
            modifier = Modifier.padding(start = padding, end = ThemeValue.Padding.HorizontalSpace - padding),
            onClick = onClick
        )
	}

	@Composable
	inline fun Actions(block: @Composable ActionScope.() -> Unit) = block()

    @Composable
    fun ActionLayout(
        modifier: Modifier = Modifier,
        block: @Composable ActionScope.() -> Unit
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = if (ltr) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Actions(block)
        }
    }
}