package love.yinlin.ui.screen.msg.pictures

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kotlinx.io.InternalIoApi
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.platform.test

@OptIn(InternalIoApi::class)
@Composable
fun ScreenPictures() {
	val a = rememberCoroutineScope()

	Text(
		text = "ScreenPictures",
		modifier = Modifier.clickable {
			test { bytes ->
				println(bytes)
				a.launch {
					val result = ClientAPI.request(
						route = API.Test.Post,
						data = API.Test.Post.Request("abc", 123),
						files = {
							API.Test.Post.Files(c = file(bytes))
						}
					)
					println(result)
				}
			}
		}
	)
}