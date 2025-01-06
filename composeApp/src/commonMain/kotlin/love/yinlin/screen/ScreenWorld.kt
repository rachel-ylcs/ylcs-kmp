package love.yinlin.screen

import androidx.compose.runtime.Composable
import com.github.panpf.sketch.AsyncImage
import love.yinlin.model.AppModel

@Composable
fun ScreenWorld(model: AppModel) {
	AsyncImage(
		uri = "https://images.pexels.com/photos/29998462/pexels-photo-29998462.jpeg?auto=compress&cs=tinysrgb&h=350",
		contentDescription = null,
	)
}