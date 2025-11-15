package love.yinlin.data.rachel.emoji

import androidx.compose.runtime.Stable
import love.yinlin.api.ServerRes
import love.yinlin.api.url

@Stable
data class Emoji(
    val id: Int,
    val type: EmojiType
) {
    private val webpPath: String by lazy { ServerRes.Emoji.webp(id).url }

    private val lottiePath: String by lazy { ServerRes.Emoji.lottie(id).url }

    val previewPath: String by lazy { webpPath }

    val showPath: String by lazy { when (type) {
        EmojiType.Static -> webpPath
        EmojiType.Dynamic -> webpPath
        EmojiType.Lottie -> lottiePath
    } }

    companion object {
        fun fromId(id: Int): Emoji? = when (id) {
            in EmojiType.Static.start..EmojiType.Static.tail -> Emoji(id, EmojiType.Static)
            in EmojiType.Dynamic.start..EmojiType.Dynamic.tail -> Emoji(id, EmojiType.Dynamic)
            in EmojiType.Lottie.start..EmojiType.Lottie.tail -> Emoji(id, EmojiType.Lottie)
            else -> null
        }
    }
}