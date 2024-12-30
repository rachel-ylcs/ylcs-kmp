package love.yinlin.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.data.TabItem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
private fun NavigationItemIcon(
	item: TabItem,
	selected: Boolean
) {
	Image(
		modifier = Modifier.size(24.dp),
		painter = painterResource(if (selected) item.iconActive else item.iconNormal),
		contentDescription = null
	)
}

@Composable
private fun NavigationItemText(
	item: TabItem,
	selected: Boolean
) {
	Text(
		text = stringResource(item.title),
		style = MaterialTheme.typography.titleMedium,
		color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
	)
}

@Composable
fun NavigationItem(
	item: TabItem,
	isSelected: Boolean,
	rowScope: RowScope? = null,
	onClick: () -> Unit
) {
	if (rowScope != null) {
		rowScope.NavigationBarItem(
			selected = isSelected,
			onClick = onClick,
			icon = { NavigationItemIcon(item, isSelected) },
			label = { NavigationItemText(item, isSelected) }
		)
	}
	else {
		NavigationRailItem(
			selected = isSelected,
			onClick = onClick,
			icon = { NavigationItemIcon(item, isSelected) },
			label = { NavigationItemText(item, isSelected) }
		)
	}
}