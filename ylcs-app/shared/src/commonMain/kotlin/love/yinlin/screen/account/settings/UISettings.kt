package love.yinlin.screen.account.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import love.yinlin.compose.*
import love.yinlin.compose.ui.image.ColorfulIcon
import love.yinlin.compose.ui.image.ColorfulImageVector
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.image.StaticLoadingIcon
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.Space

@Stable
data object SettingsScope {
	@Composable
	private fun ItemDivider(x: Dp = CustomTheme.padding.zeroSpace) {
		HorizontalDivider(modifier = Modifier.padding(horizontal = x))
	}

	@Composable
	fun Item(
		title: String,
		icon: ColorfulImageVector? = null,
		color: Color = MaterialTheme.colorScheme.onSurface,
		hasDivider: Boolean = true,
		enabled: Boolean = true,
		onClick: () -> Unit = {},
		content: @Composable () -> Unit
	) {
		Row(
			modifier = Modifier.heightIn(min = CustomTheme.padding.verticalExtraSpace * 6)
				.fillMaxWidth()
				.clickable(enabled = enabled, onClick = onClick)
				.padding(CustomTheme.padding.extraValue),
			horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
			verticalAlignment = Alignment.CenterVertically
		) {
			if (icon != null) ColorfulIcon(icon = icon)
			Text(
				text = title,
				style = MaterialTheme.typography.labelMedium,
				color = color,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			Space()
			Box(
				modifier = Modifier.weight(1f),
				contentAlignment = Alignment.CenterEnd
			) {
				content()
			}
		}

		if (hasDivider) ItemDivider()
	}

	@Composable
	fun ItemText(
		title: String,
		icon: ColorfulImageVector? = null,
		color: Color = MaterialTheme.colorScheme.onSurface,
		text: String,
		maxLines: Int = 1,
		hasDivider: Boolean = true,
		enabled: Boolean = true,
		onClick: () -> Unit = {}
	) {
		Item(
			title = title,
			icon = icon,
			color = color,
			hasDivider = hasDivider,
			enabled = enabled,
			onClick = onClick
		) {
			Text(
				text = text,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				style = MaterialTheme.typography.bodySmall,
				maxLines = maxLines,
				overflow = TextOverflow.Ellipsis
			)
		}
	}

	@Composable
	fun ItemSwitch(
		title: String,
		icon: ColorfulImageVector? = null,
		color: Color = MaterialTheme.colorScheme.onSurface,
		enabled: Boolean = true,
		hasDivider: Boolean = true,
		checked: Boolean,
		onCheckedChange: (Boolean) -> Unit = {}
	) {
		Item(
			title = title,
			icon = icon,
			color = color,
			hasDivider = hasDivider,
			enabled = enabled,
			onClick = {}
		) {
			Switch(
				checked = checked,
				onCheckedChange = onCheckedChange,
				enabled = enabled
			)
		}
	}

	@Composable
	fun ItemExpander(
		title: String,
		icon: ColorfulImageVector? = null,
		color: Color = MaterialTheme.colorScheme.onSurface,
		text: String? = null,
		hasDivider: Boolean = true,
		enabled: Boolean = true,
		onClick: () -> Unit = {}
	) {
		Item(
			title = title,
			icon = icon,
			color = color,
			hasDivider = hasDivider,
			enabled = enabled,
			onClick = onClick
		) {
			Row(
				horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
				verticalAlignment = Alignment.CenterVertically
			) {
				if (text != null) {
					Text(
						text = text,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						style = MaterialTheme.typography.bodySmall,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
				}
				MiniIcon(icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight)
			}
		}
	}

	@Composable
	fun ItemExpanderSuspend(
		title: String,
		icon: ColorfulImageVector? = null,
		color: Color = MaterialTheme.colorScheme.onSurface,
		text: String? = null,
		hasDivider: Boolean = true,
		enabled: Boolean = true,
		onClick: suspend () -> Unit = {}
	) {
		val scope = rememberCoroutineScope()
		var isLoading by rememberFalse()

		Item(
			title = title,
			icon = icon,
			color = color,
			hasDivider = hasDivider,
			enabled = enabled && !isLoading,
			onClick = {
				scope.launch {
					isLoading = true
					onClick()
					isLoading = false
				}
			}
		) {
			Row(
				horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
				verticalAlignment = Alignment.CenterVertically
			) {
				if (text != null) {
					Text(
						text = text,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						style = MaterialTheme.typography.bodySmall,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
				}
				StaticLoadingIcon(
					isLoading = isLoading,
					icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
					color = color,
					enabled = enabled
				)
			}
		}
	}
}

@Composable
fun SettingsLayout(
	modifier: Modifier = Modifier,
	title: String,
	icon: ImageVector,
	content: @Composable SettingsScope.() -> Unit
) {
	Surface(
		modifier = modifier,
		shape = MaterialTheme.shapes.large,
		shadowElevation = CustomTheme.shadow.surface
	) {
		Column(modifier = Modifier.fillMaxWidth()) {
			Row(
				modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue),
				horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
				verticalAlignment = Alignment.CenterVertically
			) {
				MiniIcon(
					icon = icon,
					color = MaterialTheme.colorScheme.primary
				)
				Text(
					text = title,
					style = MaterialTheme.typography.titleLarge,
					color = MaterialTheme.colorScheme.primary,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
			SettingsScope.content()
		}
	}
}