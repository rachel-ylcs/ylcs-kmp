package love.yinlin.common

import androidx.collection.SparseArrayCompat
import androidx.collection.forEach
import androidx.collection.valueIterator
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.resources.*
import org.jetbrains.compose.resources.DrawableResource

@Stable
sealed class Emoji(val id: Int) {
    @Stable
    class Image(id: Int, val res: DrawableResource) : Emoji(id)
    @Stable
    class Lottie(id: Int, val data: String) : Emoji(id)
}

@Stable
object EmojiManager {
    // 0 茶舍logo
    // 1 老实小狗
    // 2 瞪眼企鹅

    // 10001 - 随地大小哭

    const val IMAGE_BEGIN = 0
    const val IMAGE_END = 2
    const val LOTTIE_BEGIN = 10001
    const val LOTTIE_END = 10017

    private var emojiMap: SparseArrayCompat<Emoji> by mutableStateOf(SparseArrayCompat())

    operator fun get(id: Int): Emoji? = emojiMap[id]

    fun toList(): List<Emoji> = buildList { emojiMap.forEach { _, emoji -> add(emoji) } }

    suspend fun initialize() {
        val imageArray = arrayOf(
            Res.drawable.img_logo,
            Res.drawable.emoji1,
            Res.drawable.emoji2
        )
        emojiMap = SparseArrayCompat<Emoji>().apply {
            for (i in IMAGE_BEGIN .. IMAGE_END) {
                put(i, Emoji.Image(i, imageArray[i]))
            }
            for (i in LOTTIE_BEGIN .. LOTTIE_END) {
                put(i, Emoji.Lottie(i, Res.readBytes("files/emoji/$i.json").decodeToString()))
            }
        }
    }
}