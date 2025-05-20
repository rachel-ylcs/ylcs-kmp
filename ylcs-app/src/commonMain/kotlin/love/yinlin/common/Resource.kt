package love.yinlin.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.platform.Coroutines
import org.jetbrains.compose.resources.ExperimentalResourceApi
import love.yinlin.resources.Res

object Resource {
	var lunar: ByteArray? by mutableStateOf(null)

	@OptIn(ExperimentalResourceApi::class)
	fun initialize() {
		Coroutines.startIO {
			lunar = Res.readBytes("files/lunar.bin")
			EmojiManager.initialize()
		}
	}
}