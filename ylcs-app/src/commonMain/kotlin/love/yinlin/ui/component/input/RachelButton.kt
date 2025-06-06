package love.yinlin.ui.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.common.ThemeValue
import love.yinlin.extension.rememberFalse
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.layout.LoadingAnimation

@Composable
fun RachelText(
	text: String,
	icon: ImageVector,
	color: Color = MaterialTheme.colorScheme.onSurface,
	style: TextStyle = LocalTextStyle.current,
	padding: PaddingValues = ThemeValue.Padding.Value,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Row(
			modifier = Modifier.height(IntrinsicSize.Min).padding(padding),
			horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.CenterHorizontally),
			verticalAlignment = Alignment.CenterVertically
		) {
			Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
				Icon(
					modifier = Modifier.matchParentSize(),
					imageVector = icon,
					tint = color,
					contentDescription = null
				)
			}
			Text(
				text = text,
				style = style,
				color = color,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}

@Composable
fun RachelButton(
	text: String,
	icon: ImageVector? = null,
	color: Color = MaterialTheme.colorScheme.primary,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	TextButton(
		modifier = modifier,
		enabled = enabled,
		onClick = onClick
	) {
		Row(
			modifier = Modifier.height(IntrinsicSize.Min),
			horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.CenterHorizontally),
			verticalAlignment = Alignment.CenterVertically
		) {
			icon?.let {
				Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
					Icon(
						modifier = Modifier.matchParentSize(),
						imageVector = it,
						tint = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
						contentDescription = null
					)
				}
			}
			Text(
				text = text,
				color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
				style = MaterialTheme.typography.labelMedium,
				textAlign = TextAlign.Center,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}

@Composable
private fun LoadingButtonContent(
	isLoading: Boolean,
	text: String,
	icon: ImageVector?,
	color: Color
) {
	Row(
		modifier = Modifier.height(IntrinsicSize.Min),
		horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.CenterHorizontally),
		verticalAlignment = Alignment.CenterVertically
	) {
		if (isLoading) {
			Box(
				modifier = Modifier.fillMaxHeight().aspectRatio(1f),
				contentAlignment = Alignment.Center
			) {
				LoadingAnimation(color = color)
			}
		}
		else {
			icon?.let {
				Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
					Icon(
						modifier = Modifier.matchParentSize(),
						imageVector = it,
						tint = color,
						contentDescription = null
					)
				}
			}
		}
		Text(
			text = text,
			color = if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant else color,
			style = MaterialTheme.typography.bodyMedium,
			textAlign = TextAlign.Center,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
	}
}

@Composable
fun LoadingRachelButton(
	text: String,
	icon: ImageVector? = null,
	color: Color = MaterialTheme.colorScheme.primary,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: suspend CoroutineScope.() -> Unit
) {
	val scope = rememberCoroutineScope()
	var isLoading by rememberFalse()

	TextButton(
		modifier = modifier,
		enabled = enabled && !isLoading,
		onClick = {
			scope.launch {
				isLoading = true
				onClick()
				isLoading = false
			}
		}
	) {
		LoadingButtonContent(
			isLoading = isLoading,
			text = text,
			icon = icon,
			color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

@Composable
fun LoadingButton(
	text: String,
	icon: ImageVector? = null,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: suspend CoroutineScope.() -> Unit
) {
	val scope = rememberCoroutineScope()
	var isLoading by rememberFalse()

	Button(
		modifier = modifier,
		enabled = enabled && !isLoading,
		colors = ButtonColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer,
			contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
			disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
			disabledContentColor = MaterialTheme.colorScheme.onSurface
		),
		onClick = {
			scope.launch {
				isLoading = true
				onClick()
				isLoading = false
			}
		}
	) {
		LoadingButtonContent(
			isLoading = isLoading,
			text = text,
			icon = icon,
			color = LocalContentColor.current
		)
	}
}

@Composable
private fun ContainerButton(
	text: String,
	icon: ImageVector?,
	contentColor: Color,
	backgroundColor: Color,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier.clip(MaterialTheme.shapes.large)
			.background(color = backgroundColor)
			.clickable(onClick = onClick).padding(ThemeValue.Padding.Value),
		horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
		verticalAlignment = Alignment.CenterVertically
	) {
		icon?.let {
			MiniIcon(
				icon = it,
				color = contentColor
			)
		}
		Text(
			text = text,
			color = contentColor,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
	}
}

@Composable
fun PrimaryButton(
	text: String,
	icon: ImageVector? = null,
	onClick: () -> Unit
) {
	ContainerButton(
		text = text,
		icon = icon,
		contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
		backgroundColor = MaterialTheme.colorScheme.primaryContainer,
		onClick = onClick
	)
}

@Composable
fun SecondaryButton(
	text: String,
	icon: ImageVector? = null,
	onClick: () -> Unit
) {
	ContainerButton(
		text = text,
		icon = icon,
		contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
		backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
		onClick = onClick
	)
}

@Composable
fun TertiaryButton(
	text: String,
	icon: ImageVector? = null,
	onClick: () -> Unit
) {
	ContainerButton(
		text = text,
		icon = icon,
		contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
		backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
		onClick = onClick
	)
}

@Composable
fun RachelRadioButton(
	checked: Boolean,
	text: String,
	onCheck: () -> Unit,
	enabled: Boolean = true,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier.height(IntrinsicSize.Min).selectable(
			selected = checked,
			onClick = onCheck,
			enabled = enabled,
			role = Role.RadioButton
		).padding(ThemeValue.Padding.Value),
		horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
		verticalAlignment = Alignment.CenterVertically
	) {
		RadioButton(
			selected = checked,
			enabled = enabled,
			colors = RadioButtonColors(
				selectedColor = MaterialTheme.colorScheme.primary,
				unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
				disabledSelectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
				disabledUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
			),
			onClick = null
		)
		Text(
			text = text,
			color = LocalContentColor.current
		)
	}
}