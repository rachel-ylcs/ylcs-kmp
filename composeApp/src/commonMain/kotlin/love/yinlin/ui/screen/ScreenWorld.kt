package love.yinlin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.ui.component.extra.WebPage
import love.yinlin.ui.component.extra.WebPageSettings
import love.yinlin.ui.component.extra.rememberWebPageState

@Composable
fun ScreenWorld() {
	val state = rememberWebPageState { WebPageSettings() }
	Column(modifier = Modifier.fillMaxSize()) {
		WebPage(
			state = state,
			modifier = Modifier.fillMaxWidth().weight(1f)
		)
	}

	LaunchedEffect(Unit) {
		state.url = "https://www.baidu.com"
	}
}