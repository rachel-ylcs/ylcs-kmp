package love.yinlin.data.information

import androidx.compose.runtime.Stable

/**
 * 统一用户信息接口
 */
@Stable
interface UnifiedUserInfo {
    val id: String // ID
    val name: String // 昵称
    val avatar: String // 头像
}