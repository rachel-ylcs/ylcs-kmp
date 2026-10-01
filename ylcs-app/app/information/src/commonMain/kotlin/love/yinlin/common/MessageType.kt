package love.yinlin.common

import androidx.compose.runtime.Stable

@Stable
enum class MessageType {
    Weibo,
    Chaohua,
    Douyin,
    RedBook;

    companion object {
        val Default = Weibo
    }
}