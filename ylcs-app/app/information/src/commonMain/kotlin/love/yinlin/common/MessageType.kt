package love.yinlin.common

import androidx.compose.runtime.Stable

@Stable
enum class MessageType {
    Weibo, // 微博
    Chaohua, // 超话
    Douyin; // 抖音

    companion object {
        val Default = Weibo
    }
}