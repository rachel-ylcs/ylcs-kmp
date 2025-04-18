package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.extension.LaunchOnce
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.screen.msg.ScreenPartMsg

@Composable
fun ScreenChaohua(part: ScreenPartMsg) {
	StatefulBox(
		state = part.chaohuaState.grid.state,
		modifier = Modifier.fillMaxSize()
	) {
		PaginationStaggeredGrid(
			items = part.chaohuaState.grid.items,
			key = { it.id },
			columns = StaggeredGridCells.Adaptive(300.dp),
			canRefresh = true,
			canLoading = part.chaohuaState.canLoading,
			onRefresh = { part.chaohuaState.requestNewData() },
			onLoading = { part.chaohuaState.requestMoreData() },
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(10.dp),
			horizontalArrangement = Arrangement.spacedBy(10.dp),
			verticalItemSpacing = 10.dp
		) { weibo ->
			WeiboCard(
				weibo = weibo,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}

	LaunchOnce(part.chaohuaState.flagFirstLoad) {
		part.chaohuaState.requestNewData()
	}
}