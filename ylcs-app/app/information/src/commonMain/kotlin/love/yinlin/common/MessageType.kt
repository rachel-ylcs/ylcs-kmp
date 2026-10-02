package love.yinlin.common

import androidx.compose.runtime.Stable

@Stable
enum class MessageType {
    Weibo,
    Chaohua,
    Bilibili,
    RedBook,
    Douyin;

    companion object {
        val Default = Weibo
    }
}