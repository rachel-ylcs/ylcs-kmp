package love.yinlin.ui.component.extra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import love.yinlin.ui.component.image.MiniIcon

@Stable
actual class WebPageState actual constructor(val settings: WebPageSettings, initUrl: String) {
	actual var url: String = initUrl

	actual val loadingState: WebPageLoadingState get() = WebPageLoadingState.Initializing

	actual val title: String get() = ""

	actual val icon: BitmapPainter? get() = null

	actual val canGoBack: Boolean get() = false

	actual val canGoForward: Boolean get() = false

	actual val error: WebPageError? get() = null

	actual fun goBack() { }
	actual fun goForward() { }
	actual fun evaluateJavaScript(script: String) { }
}

@Composable
actual fun WebPage(
	state: WebPageState,
	modifier: Modifier
) {
	Surface(
		modifier = modifier,
		shadowElevation = 5.dp
	) {
		Column(
			modifier = modifier,
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
		) {
			MiniIcon(
				imageVector = Icons.Filled.NotificationImportant,
				size = 50.dp
			)
			Text(text = "该组件或功能未在此平台实现")
		}
	}
}