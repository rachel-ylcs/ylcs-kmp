package love.yinlin.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yinlin.common.ThemeColor
import love.yinlin.ui.component.image.ColorfulIcon
import love.yinlin.ui.component.image.ColorfulImageVector
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.layout.Space

object SettingsScope {
	@Composable
	private fun ItemDivider(
		x: Dp = 10.dp,
		y: Dp = 1.dp
	) {
		HorizontalDivider(modifier = Modifier.padding(horizontal = x, vertical = y))
	}

	@Composable
	fun Item(
		title: String,
		icon: ColorfulImageVector? = null,
		color: Color = MaterialTheme.colorScheme.onSurface,
		hasDivider: Boolean = true,
		onClick: () -> Unit = {},
		content: @Composable () -> Unit
	) {
		Row(
			modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 10.dp),
			horizontalArrangement = Arrangement.spacedBy(10.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			if (icon != null) ColorfulIcon(imageVector = icon)
			Text(
				text = title,
				style = MaterialTheme.typography.labelLarge,
				color = color,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			Space(10.dp)
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
		onClick: () -> Unit = {}
	) {
		Item(
			title = title,
			icon = icon,
			color = color,
			hasDivider = hasDivider,
			onClick = onClick
		) {
			Text(
				text = text,
				color = ThemeColor.fade,
				maxLines = maxLines,
				overflow = TextOverflow.Ellipsis
			)
		}
	}

	@Composable
	fun ItemExpander(
		title: String,
		icon: ColorfulImageVector? = null,
		color: Color = MaterialTheme.colorScheme.onSurface,
		hasDivider: Boolean = true,
		onClick: () -> Unit = {}
	) {
		Item(
			title = title,
			icon = icon,
			color = color,
			hasDivider = hasDivider,
			onClick = onClick
		) {
			MiniIcon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight)
		}
	}

	@Composable
	fun ItemTextExpander(
		title: String,
		icon: ColorfulImageVector? = null,
		color: Color = MaterialTheme.colorScheme.onSurface,
		text: String,
		maxLines: Int = 1,
		hasDivider: Boolean = true,
		onClick: () -> Unit = {}
	) {
		Item(
			title = title,
			icon = icon,
			color = color,
			hasDivider = hasDivider,
			onClick = onClick
		) {
			Row(
				horizontalArrangement = Arrangement.spacedBy(10.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = text,
					color = ThemeColor.fade,
					maxLines = maxLines,
					overflow = TextOverflow.Ellipsis
				)
				MiniIcon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight)
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
		shadowElevation = 3.dp
	) {
		Column(modifier = Modifier.fillMaxWidth()) {
			Row(
				modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 20.dp),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				MiniIcon(
					imageVector = icon,
					color = MaterialTheme.colorScheme.primary
				)
				Text(
					text = title,
					style = MaterialTheme.typography.displayMedium,
					color = MaterialTheme.colorScheme.primary,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
			SettingsScope.content()
		}
	}
}