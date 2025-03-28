package love.yinlin.data.rachel

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class Mail(
	val mid: Long, // [邮件 ID]
	val uid: Int, // [用户 ID]
	val ts: String, // [发布时间]
	val type: Int, // [邮件类型]
	val processed: Boolean, // [是否处理]
	val title: String, // [标题]
	val content: String // [内容]
	// filter: String [邮件处理器]
	// param1: String? [处理参数1]
	// param2: String? [处理参数2]
	// param3: String? [处理参数3]
	// info: JsonObject? [其他处理参数]
) {
	object Type {
		const val INFO = 1
		const val CONFIRM = 2
		const val DECISION = 4
		const val INPUT = 8
	}

	object Filter {
		const val REGISTER = "user#register"
		const val FORGOT_PASSWORD = "user#forgotPassword"
		const val COIN_REWARD = "user#coinReward"
	}

	val typeString: String by lazy {
		when (type) {
			Type.INFO -> "通知"
			Type.CONFIRM -> "确认"
			Type.DECISION -> "决策"
			Type.INPUT -> "回执"
			else -> "未知"
		}
	}

	val withYes: Boolean by lazy {
		!processed && (type == Type.CONFIRM || type == Type.DECISION)
	}

	val withNo: Boolean by lazy {
		!processed && type == Type.DECISION
	}
}