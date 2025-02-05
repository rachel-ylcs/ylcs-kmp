package love.yinlin.ui.screen.msg.weibo

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import love.yinlin.AppModel

@Composable
fun ScreenChaohua() {
	var text by remember { mutableStateOf("ScreenChaohua") }
	OutlinedTextField(
		value = text,
		onValueChange = { text = it }
	)
}