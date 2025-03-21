package love.yinlin.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.ExperimentalResourceApi
import love.yinlin.resources.Res

object Resource {
	var lunar: ByteArray? by mutableStateOf(null)

	@OptIn(ExperimentalResourceApi::class)
	suspend fun initialize() {
		lunar = Res.readBytes("files/lunar.bin")
	}
}