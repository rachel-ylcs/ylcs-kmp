package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.extension.LaunchOnce
import love.yinlin.platform.app
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.screen.msg.ScreenPartMsg

@Composable
fun ScreenWeibo(part: ScreenPartMsg) {
	StatefulBox(
		state = part.weiboState.grid.state,
		modifier = Modifier.fillMaxSize()
	) {
		WeiboGrid(
			items = part.weiboState.grid.items,
			modifier = Modifier.fillMaxSize()
		)
	}

	LaunchOnce(part.weiboState.flagFirstLoad) {
		part.weiboState.grid.requestWeibo(app.config.weiboUsers.map { it.id })
	}
}