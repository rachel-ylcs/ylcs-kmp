package love.yinlin.ui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.ui.Screen
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.screen.SubScreen

@Composable
fun UnsupportedComponent(modifier: Modifier = Modifier) {
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

@Composable
fun <M : Screen.Model> UnsupportedScreen(model: M) {
	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = "该组件或功能未在此平台实现",
		onBack = { model.pop() }
	) {
		UnsupportedComponent(modifier = Modifier.fillMaxSize())
	}
}