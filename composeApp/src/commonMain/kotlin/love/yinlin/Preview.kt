package love.yinlin

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun preview() {
	RachelTheme(
		darkMode = false
	) {
		Text(text = "你好")
	}
}