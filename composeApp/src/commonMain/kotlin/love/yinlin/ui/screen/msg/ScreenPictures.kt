package love.yinlin.ui.screen.msg

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import love.yinlin.AppModel

@Composable
fun ScreenPictures() {
	var text by remember { mutableStateOf("ScreenPictures") }
	OutlinedTextField(
		value = text,
		onValueChange = { text = it }
	)
}