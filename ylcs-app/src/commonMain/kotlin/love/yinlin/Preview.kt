package love.yinlin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.MoveUp
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import love.yinlin.common.Colors
import love.yinlin.common.RachelTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
private fun IconText(
	icon: ImageVector,
	text: String,
	color: Color
) {
	Row(
		horizontalArrangement = Arrangement.spacedBy(5.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(imageVector = icon, contentDescription = "", tint = color)
		Text(text = text, color = color)
	}
}

@Composable
private fun ColorSolution(isDarkMode: Boolean) {
	Column(
		modifier = Modifier.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.padding(10.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(20.dp)
		) {
			Column(
				modifier = Modifier.weight(1f),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				IconText(icon = Icons.Outlined.Photo, text = "背景上文字", color = MaterialTheme.colorScheme.onBackground)
				IconText(icon = Icons.Outlined.FindReplace, text = "第一主题色", color = MaterialTheme.colorScheme.primary)
				IconText(icon = Icons.Outlined.MoveUp, text = "第二主题色", color = MaterialTheme.colorScheme.secondary)
				IconText(icon = Icons.Outlined.AcUnit, text = "第三主题色", color = MaterialTheme.colorScheme.tertiary)
			}
			Column(
				modifier = Modifier.weight(1f),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				Box(
					modifier = Modifier.fillMaxWidth()
						.background(MaterialTheme.colorScheme.primaryContainer)
						.padding(horizontal = 10.dp, vertical = 5.dp),
					contentAlignment = Alignment.Center
				) {
					IconText(icon = Icons.Outlined.FindReplace, text = "第一容器", color = MaterialTheme.colorScheme.onPrimaryContainer)
				}
				Box(
					modifier = Modifier.fillMaxWidth()
						.background(MaterialTheme.colorScheme.secondaryContainer)
						.padding(horizontal = 10.dp, vertical = 5.dp),
					contentAlignment = Alignment.Center
				) {
					IconText(icon = Icons.Outlined.MoveUp, text = "第二容器", color = MaterialTheme.colorScheme.onSecondaryContainer)
				}
				Box(
					modifier = Modifier.fillMaxWidth()
						.background(MaterialTheme.colorScheme.tertiaryContainer)
						.padding(horizontal = 10.dp, vertical = 5.dp),
					contentAlignment = Alignment.Center
				) {
					IconText(icon = Icons.Outlined.AcUnit, text = "第三容器", color = MaterialTheme.colorScheme.onTertiaryContainer)
				}
			}
		}
		Surface(
			modifier = Modifier.fillMaxWidth(),
			shape = MaterialTheme.shapes.extraLarge,
			shadowElevation = 5.dp
		) {
			Column(
				modifier = Modifier.fillMaxWidth().padding(10.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(20.dp)
				) {
					Column(
						modifier = Modifier.weight(1f),
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.spacedBy(10.dp)
					) {
						IconText(icon = Icons.Outlined.Photo, text = "表面文字", color = MaterialTheme.colorScheme.onSurface)
						IconText(icon = Icons.Outlined.Photo, text = "文字变体", color = MaterialTheme.colorScheme.onSurfaceVariant)
					}
					Column(
						modifier = Modifier.weight(1f),
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.spacedBy(10.dp)
					) {
						IconText(icon = Icons.Outlined.FindReplace, text = "第一主题", color = MaterialTheme.colorScheme.primary)
						IconText(icon = Icons.Outlined.MoveUp, text = "第二主题", color = MaterialTheme.colorScheme.secondary)
						IconText(icon = Icons.Outlined.AcUnit, text = "第三主题", color = MaterialTheme.colorScheme.tertiary)
					}
				}
			}
		}
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(20.dp)
		) {
			Box(
				modifier = Modifier.weight(1f)
					.background(MaterialTheme.colorScheme.error)
					.padding(horizontal = 10.dp, vertical = 5.dp)
			) {
				IconText(icon = Icons.Outlined.Error, text = "错误", color = MaterialTheme.colorScheme.onError)
			}
			Box(
				modifier = Modifier.weight(1f)
					.background(if (isDarkMode) Colors.Yellow4 else Colors.Yellow5)
					.padding(horizontal = 10.dp, vertical = 5.dp)
			) {
				IconText(icon = Icons.Outlined.Warning, text = "警告", color = if (isDarkMode) Colors.Ghost else Colors.White)
			}
		}
	}
}

@Preview
@Composable
fun preview() {
	Column(modifier = Modifier.width(300.dp).height(640.dp)) {
		RachelTheme(darkMode = false) {
			CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.displaySmall) {
				Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
					ColorSolution(false)
				}
			}

		}
		RachelTheme(darkMode = true) {
			CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.displaySmall) {
				Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
					ColorSolution(true)
				}
			}
		}
	}
}