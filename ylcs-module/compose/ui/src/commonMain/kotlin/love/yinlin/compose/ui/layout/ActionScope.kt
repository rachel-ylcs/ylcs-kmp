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
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.ui.floating.BallonTip
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.image.ClickImage
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.LoadingImage

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
        useImage: Boolean = false,
        onClick: () -> Unit
	) {
		val padding = if (ltr) {
            CustomTheme.padding.horizontalSpace
        } else {
            CustomTheme.padding.zeroSpace
        }

        if (useImage) {
            ClickImage(
                icon = icon,
                enabled = enabled,
                modifier = Modifier.padding(start = padding, end = CustomTheme.padding.horizontalSpace - padding),
                onClick = onClick
            )
        }
        else {
            ClickIcon(
                icon = icon,
                color = color,
                enabled = enabled,
                modifier = Modifier.padding(start = padding, end = CustomTheme.padding.horizontalSpace - padding),
                onClick = onClick
            )
        }
	}

    @Composable
    fun Action(
        icon: ImageVector,
        tip: String,
        color: Color = MaterialTheme.colorScheme.onSurface,
        enabled: Boolean = true,
        useImage: Boolean = false,
        onClick: () -> Unit
    ) {
        BallonTip(text = tip) { Action(icon, color, enabled, useImage, onClick) }
    }

	@Composable
	fun ActionSuspend(
        icon: ImageVector,
        color: Color = MaterialTheme.colorScheme.onSurface,
        enabled: Boolean = true,
        useImage: Boolean = false,
        onClick: suspend () -> Unit
	) {
		val padding = if (ltr) {
            CustomTheme.padding.horizontalSpace
        } else {
            CustomTheme.padding.zeroSpace
        }

        if (useImage) {
            LoadingImage(
                icon = icon,
                color = color,
                enabled = enabled,
                modifier = Modifier.padding(start = padding, end = CustomTheme.padding.horizontalSpace - padding),
                onClick = onClick
            )
        }
        else {
            LoadingIcon(
                icon = icon,
                color = color,
                enabled = enabled,
                modifier = Modifier.padding(start = padding, end = CustomTheme.padding.horizontalSpace - padding),
                onClick = onClick
            )
        }
	}

    @Composable
    fun ActionSuspend(
        icon: ImageVector,
        tip: String,
        color: Color = MaterialTheme.colorScheme.onSurface,
        enabled: Boolean = true,
        useImage: Boolean = false,
        onClick: suspend () -> Unit
    ) {
        BallonTip(text = tip) { ActionSuspend(icon, color, enabled, useImage, onClick) }
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