package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.common.ThemeValue
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.screen.msg.ScreenPartMsg

@Composable
fun PageChaohua(
	part: ScreenPartMsg,
	state: LazyStaggeredGridState
) {
	StatefulBox(
		state = part.chaohuaState.grid.state,
		modifier = Modifier.fillMaxSize()
	) {
		PaginationStaggeredGrid(
			items = part.chaohuaState.grid.items,
			key = { it.id },
			columns = StaggeredGridCells.Adaptive(ThemeValue.Size.CardWidth),
			state = state,
			canRefresh = true,
			canLoading = part.chaohuaState.canLoading,
			onRefresh = { part.chaohuaState.requestNewData() },
			onLoading = { part.chaohuaState.requestMoreData() },
			modifier = Modifier.fillMaxSize(),
			contentPadding = ThemeValue.Padding.EqualValue,
			horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
			verticalItemSpacing = ThemeValue.Padding.EqualSpace
		) { weibo ->
			WeiboCard(
				weibo = weibo,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}