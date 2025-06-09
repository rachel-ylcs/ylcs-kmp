package love.yinlin.data.rachel.emoji

import androidx.compose.runtime.Stable

@Stable
enum class EmojiType(
    val title: String,
    val start: Int,
    val end: Int,
    val tail: Int
) {
    Static("经典", 0, 3, 1000), // 静态区块
    Dynamic("动图", 1001, 1008, 2000), // 动态区块
    Lottie("黄脸", 2001, 2038, 3000); // LOTTIE区块

    companion object {
        fun fromInt(value: Int): EmojiType = when (value) {
            Static.ordinal -> Static
            Dynamic.ordinal -> Dynamic
            Lottie.ordinal -> Lottie
            else -> error("Unknown type: $value")
        }
    }
}