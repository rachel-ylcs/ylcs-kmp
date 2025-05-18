package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.screen.msg.ScreenPartMsg

@Composable
fun PageWeibo(
	part: ScreenPartMsg,
	state: LazyStaggeredGridState
) {
	StatefulBox(
		state = part.weiboState.grid.state,
		modifier = Modifier.fillMaxSize()
	) {
		WeiboGrid(
			state = state,
			items = part.weiboState.grid.items,
			modifier = Modifier.fillMaxSize()
		)
	}
}