package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString

/**
 * 一份导航条目的创建描述，不包含 Screen 实例及其当前状态。
 *
 * [type] 用于工厂查找和同类型策略匹配；[id] 标识这一份页面实例；
 * [args] 只用于首次创建，不能在 Resume 或 UI 重建时覆盖页面当前 state。
 *
 */
@Stable
@Serializable
internal data class ScreenKey(
    val type: String,
    val args: ScreenArgs = ScreenArgs.Empty,
    @Required val id: ScreenID = ScreenID(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val isUnboundKey: Boolean = false
) {
    init {
        require(type.isNotBlank())
    }

    override fun toString(): String = toJsonString()

    companion object {
        fun parse(json: String): ScreenKey = json.parseJsonValue()
    }
}
