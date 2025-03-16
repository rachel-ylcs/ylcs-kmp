package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.extension.LaunchOnce
import love.yinlin.platform.app
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.screen.msg.MsgModelPart

@Composable
fun ScreenWeibo(model: MsgModelPart) {
	StatefulBox(
		state = model.weiboState.grid.state,
		modifier = Modifier.fillMaxSize()
	) {
		WeiboGrid(
			items = model.weiboState.grid.items,
			modifier = Modifier.fillMaxSize()
		)
	}

	LaunchOnce(model.weiboState.flagFirstLoad) {
		model.weiboState.grid.requestWeibo(app.config.weiboUsers.map { it.id })
	}
}