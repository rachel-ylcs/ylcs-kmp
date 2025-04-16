package love.yinlin.ui.component.layout

import androidx.compose.foundation.clickable
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
					height = 3.dp,
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
				).padding(horizontal = 15.dp, vertical = 10.dp),
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
			horizontalArrangement = Arrangement.spacedBy(10.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			MiniIcon(
				icon = icon,
				size = 20.dp,
				color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
			)
			Text(
				text = title,
				color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
				style = if (isSelected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge,
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
			style = if (isSelected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge,
			textAlign = TextAlign.Center
		)
	}
}