package love.yinlin.screen.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.Colors
import love.yinlin.data.weibo.Weibo

@Composable
fun WeiboCard(
	weibo: Weibo,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	ElevatedCard(
		modifier = modifier,
		onClick = onClick
	) {
		Row {

		}
		Text(weibo.text, modifier = Modifier.background(Colors.Green2))
	}
}

@Composable
fun WeiboGrid(
	modifier: Modifier = Modifier,
	items: List<Weibo>,
	onClick: (Weibo) -> Unit
) {
	LazyVerticalStaggeredGrid(
		columns = StaggeredGridCells.Adaptive(300.dp),
		modifier = modifier,
		contentPadding = PaddingValues(10.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		verticalItemSpacing = 10.dp
	) {
		items(
			items = items,
			key = { it.id }
		) {
			WeiboCard(
				weibo = it
			) {
				onClick(it)
			}
		}
	}
}