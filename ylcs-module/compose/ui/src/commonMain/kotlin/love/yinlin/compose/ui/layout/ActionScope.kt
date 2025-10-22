package love.yinlin.compose.ui.layout

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
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.ui.floating.BallonTip
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.image.LoadingIcon

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
		val padding = if (ltr) CustomTheme.padding.horizontalSpace else CustomTheme.padding.zeroSpace

        ClickIcon(
            icon = icon,
            color = color,
            enabled = enabled,
            modifier = Modifier.Companion.padding(start = padding, end = CustomTheme.padding.horizontalSpace - padding),
            onClick = onClick
        )
	}

    @Composable
    fun Action(
        icon: ImageVector,
        tip: String,
        color: Color = MaterialTheme.colorScheme.onSurface,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        BallonTip(text = tip) { Action(icon, color, enabled, onClick) }
    }

	@Composable
	fun ActionSuspend(
        icon: ImageVector,
        color: Color = MaterialTheme.colorScheme.onSurface,
        enabled: Boolean = true,
        onClick: suspend CoroutineScope.() -> Unit
	) {
		val padding = if (ltr) CustomTheme.padding.horizontalSpace else CustomTheme.padding.zeroSpace

        LoadingIcon(
            icon = icon,
            color = color,
            enabled = enabled,
            modifier = Modifier.Companion.padding(start = padding, end = CustomTheme.padding.horizontalSpace - padding),
            onClick = onClick
        )
	}

    @Composable
    fun ActionSuspend(
        icon: ImageVector,
        tip: String,
        color: Color = MaterialTheme.colorScheme.onSurface,
        enabled: Boolean = true,
        onClick: suspend CoroutineScope.() -> Unit
    ) {
        BallonTip(text = tip) { ActionSuspend(icon, color, enabled, onClick) }
    }

	@Composable
	inline fun Actions(block: @Composable ActionScope.() -> Unit) = block()

    @Composable
    fun ActionLayout(
        modifier: Modifier = Modifier.Companion,
        block: @Composable ActionScope.() -> Unit
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = if (ltr) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Actions(block)
        }
    }
}