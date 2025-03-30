package love.yinlin.data.rachel.mail

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Stable
@Serializable
data class MailEntry(
	val uid: Int, // [用户 ID]
	val processed: Boolean, // [是否处理]
	val filter: String, // [邮件处理器]
	val param1: String?, // [处理参数1]
	val param2: String?, // [处理参数2]
	val param3: String?, // [处理参数3]
	val info: JsonObject? // [其他处理参数]
)