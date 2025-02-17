package love.yinlin.ui.screen.msg.pictures

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import love.yinlin.extension.rememberState

@Composable
fun ScreenPictures() {
	var text by rememberState { "ScreenPictures" }
	OutlinedTextField(
		value = text,
		onValueChange = { text = it }
	)
}