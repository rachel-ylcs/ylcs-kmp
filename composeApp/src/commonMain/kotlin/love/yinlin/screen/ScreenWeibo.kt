package love.yinlin.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import love.yinlin.component.StatefulBox
import love.yinlin.model.AppModel
import love.yinlin.screen.common.WeiboGrid

@Composable
fun ScreenWeibo(model: AppModel) {
	val weiboState = model.msgModel.weiboState

	StatefulBox(weiboState.state) {
		WeiboGrid(
			modifier = Modifier.fillMaxSize(),
			items = weiboState.items
		) {

		}
	}

	LaunchedEffect(true) {
		if (weiboState.isFirstLoad) weiboState.requestWeibo()
	}
}