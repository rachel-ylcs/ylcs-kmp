package love.yinlin.screen

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import love.yinlin.model.AppModel

@Composable
fun ScreenPictures(model: AppModel) {
	var text by rememberSaveable { mutableStateOf("ScreenPictures") }
	OutlinedTextField(
		value = text,
		onValueChange = { text = it }
	)
}