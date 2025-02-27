package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.extension.LaunchOnce
import love.yinlin.platform.config
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.screen.msg.MsgModel

@Composable
fun ScreenWeibo(model: MsgModel) {
	StatefulBox(
		state = model.weiboState.grid.state,
		modifier = Modifier.fillMaxSize()
	) {
		WeiboGrid(
			model = model,
			items = model.weiboState.grid.items,
			modifier = Modifier.fillMaxSize()
		)
	}

	LaunchOnce(model.weiboState.launchFlag) {
		model.weiboState.grid.requestWeibo(config.weiboUsers.map { it.id })
	}
}