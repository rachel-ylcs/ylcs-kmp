package love.yinlin.data.rachel.emoji

import androidx.compose.runtime.Stable
import love.yinlin.Local
import love.yinlin.api.ServerRes

@Stable
data class Emoji(
    val id: Int,
    val type: EmojiType
) {
    private val webpPath: String by lazy { "${Local.API_BASE_URL}/${ServerRes.Emoji.webp(id)}" }

    private val lottiePath: String by lazy { "${Local.API_BASE_URL}/${ServerRes.Emoji.lottie(id)}" }

    val previewPath: String by lazy { webpPath }

    val showPath: String by lazy { when (type) {
        Static -> webpPath
        Dynamic -> webpPath
        Lottie -> lottiePath
    } }

    companion object {
        fun fromId(id: Int): Emoji? = when (id) {
            in EmojiType.Static.start..EmojiType.Static.tail -> Emoji(id, Static)
            in EmojiType.Dynamic.start..EmojiType.Dynamic.tail -> Emoji(id, Dynamic)
            in EmojiType.Lottie.start..EmojiType.Lottie.tail -> Emoji(id, Lottie)
            else -> null
        }
    }
}