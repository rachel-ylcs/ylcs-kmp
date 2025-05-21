package love.yinlin.common

import androidx.collection.SparseArrayCompat
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastForEach
import love.yinlin.resources.*
import org.jetbrains.compose.resources.DrawableResource

@Stable
sealed class Emoji(val id: Int, val res: DrawableResource) {
    @Stable
    class Static(id: Int, res: DrawableResource) : Emoji(id, res)
    @Stable
    class Dynamic(id: Int, res: DrawableResource) : Emoji(id, res)
    @Stable
    class Lottie(id: Int, res: DrawableResource, val data: String) : Emoji(id, res)

    companion object {
        suspend fun Lottie(id: Int, res: DrawableResource) = Lottie(id, res, Res.readBytes("files/emoji/$id.json").decodeToString())
    }
}

@Stable
data class EmojiClassification(val title: String, val items: List<Emoji>)

@Stable
object EmojiManager {
    // 静态区块 0 - 1000
    const val STATIC_BEGIN = 0
    const val STATIC_CLASSICS_BEGIN = STATIC_BEGIN
    // 动态区块 1001 - 2000
    const val DYNAMIC_BEGIN = 1001
    const val DYNAMIC_CLASSICS_BEGIN = DYNAMIC_BEGIN
    // LOTTIE区块 2001 - 3000
    const val LOTTIE_BEGIN = 2001
    const val LOTTIE_QQ_BEGIN = LOTTIE_BEGIN

    private var emojiMap: SparseArrayCompat<Emoji> by mutableStateOf(SparseArrayCompat())
    var classifyMap: List<EmojiClassification> by mutableStateOf(emptyList())
        private set

    operator fun get(id: Int): Emoji? = emojiMap[id]

    fun MutableList<EmojiClassification>.initializeStatic() {
        add(EmojiClassification("经典", arrayOf(
            Res.drawable.img_logo,
            Res.drawable.emoji1,
            Res.drawable.emoji2
        ).mapIndexed { index, res -> Emoji.Static(STATIC_CLASSICS_BEGIN + index, res) }))
    }

    fun MutableList<EmojiClassification>.initializeDynamic() {
        add(EmojiClassification("动图", arrayOf(
            Res.drawable.emoji1001
        ).mapIndexed { index, res -> Emoji.Dynamic(DYNAMIC_CLASSICS_BEGIN + index, res) }))
    }

    suspend fun MutableList<EmojiClassification>.initializeLottie() {
        add(EmojiClassification("黄脸", arrayOf(
            Res.drawable.emoji2001,
            Res.drawable.emoji2002,
            Res.drawable.emoji2003,
            Res.drawable.emoji2004,
            Res.drawable.emoji2005,
            Res.drawable.emoji2006,
            Res.drawable.emoji2007,
            Res.drawable.emoji2008,
            Res.drawable.emoji2009,
            Res.drawable.emoji2010,
            Res.drawable.emoji2011,
            Res.drawable.emoji2012,
            Res.drawable.emoji2013,
            Res.drawable.emoji2014,
            Res.drawable.emoji2015,
            Res.drawable.emoji2016,
            Res.drawable.emoji2017
        ).mapIndexed { index, res -> Emoji.Lottie(LOTTIE_QQ_BEGIN + index, res) }))
    }

    suspend fun initialize() {
        classifyMap = buildList {
            initializeStatic()
            initializeDynamic()
            initializeLottie()
        }
        emojiMap = SparseArrayCompat<Emoji>().apply {
            classifyMap.fastForEach { list ->
                list.items.fastForEach { emoji -> put(emoji.id, emoji) }
            }
        }
    }
}