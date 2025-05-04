package love.yinlin.ui.component.layout

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import love.yinlin.common.ThemeValue
import love.yinlin.ui.component.image.MiniIcon
import kotlin.jvm.JvmName

@Composable
private fun <T> TabBar(
	currentPage: Int,
	onNavigate: (Int) -> Unit,
	onLongClick: ((Int) -> Unit)? = null,
	items: List<T>,
	modifier: Modifier = Modifier,
	content: @Composable (Boolean, T) -> Unit,
) {
	ScrollableTabRow(
		modifier = modifier,
		selectedTabIndex = currentPage,
		edgePadding = 0.dp,
		indicator = { tabPositions ->
			if (currentPage < tabPositions.size) {
				TabRowDefaults.SecondaryIndicator(
					modifier = Modifier.tabIndicatorOffset(tabPositions[currentPage]),
					height = ThemeValue.Padding.LittleSpace,
					color = MaterialTheme.colorScheme.primary
				)
			}
		},
		divider = {}
	) {
		items.forEachIndexed { index, item ->
			val isSelected = currentPage == index
			Box(
				modifier = Modifier.combinedClickable(
					onClick = {
						if (!isSelected) onNavigate(index)
					},
					onLongClick = {
						if (currentPage == index) onLongClick?.invoke(index)
					}
				).padding(ThemeValue.Padding.Value),
				contentAlignment = Alignment.Center
			) {
				content(isSelected, item)
			}
		}
	}
}

@JvmName("TabBarWithIcon")
@Composable
fun TabBar(
	currentPage: Int,
	onNavigate: (Int) -> Unit,
	onLongClick: ((Int) -> Unit)? = null,
	items: List<Pair<String, ImageVector>>,
	modifier: Modifier = Modifier
) {
	TabBar(
		currentPage = currentPage,
		onNavigate = onNavigate,
		onLongClick = onLongClick,
		items = items,
		modifier = modifier
	) { isSelected, (title, icon) ->
		Row(
			horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
			verticalAlignment = Alignment.CenterVertically
		) {
			MiniIcon(
				icon = icon,
				color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
			)
			Text(
				text = title,
				color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
				style = if (isSelected) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
				textAlign = TextAlign.Center
			)
		}
	}
}

@Composable
fun TabBar(
	currentPage: Int,
	onNavigate: (Int) -> Unit,
	onLongClick: ((Int) -> Unit)? = null,
	items: List<String>,
	modifier: Modifier = Modifier
) {
	TabBar(
		currentPage = currentPage,
		onNavigate = onNavigate,
		onLongClick = onLongClick,
		items = items,
		modifier = modifier
	) { isSelected, title ->
		Text(
			text = title,
			color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
			style = if (isSelected) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
			textAlign = TextAlign.Center
		)
	}
}