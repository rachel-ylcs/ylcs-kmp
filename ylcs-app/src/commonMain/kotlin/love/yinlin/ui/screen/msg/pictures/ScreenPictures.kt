package love.yinlin.ui.screen.msg.pictures

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun ScreenPictures() {
	val scope = rememberCoroutineScope()
	Text(text = "Screen Pictures", modifier = Modifier.clickable {
		scope.launch {

		}
	})
}