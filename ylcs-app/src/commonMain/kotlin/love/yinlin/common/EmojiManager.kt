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
    class Dynamic(id: Int, res: DrawableResource) : Emoji(id, res) {
        val resPath: String get() = Res.getUri("drawable/emoji$id.webp")
    }
    @Stable
    class Lottie(id: Int, res: DrawableResource) : Emoji(id, res)
}

@Stable
data class EmojiClassification(val title: String, val items: List<Emoji>)

@Stable
object EmojiManager {
    // 静态区块 0 - 1000
    const val STATIC_BEGIN = 0
    const val STATIC_LOGO = STATIC_BEGIN
    const val STATIC_CLASSICS_BEGIN = STATIC_LOGO + 1
    const val STATIC_CLASSICS_END = 2
    // 动态区块 1001 - 2000
    const val DYNAMIC_BEGIN = 1001
    const val DYNAMIC_CLASSICS_BEGIN = DYNAMIC_BEGIN
    const val DYNAMIC_CLASSICS_END = 1001
    // LOTTIE区块 2001 - 3000
    const val LOTTIE_BEGIN = 2001
    const val LOTTIE_QQ_BEGIN = LOTTIE_BEGIN
    const val LOTTIE_QQ_END = 2038

    private var emojiMap: SparseArrayCompat<Emoji> by mutableStateOf(SparseArrayCompat())
    var classifyMap: List<EmojiClassification> by mutableStateOf(emptyList())
        private set
    var lottieMap: ByteArray by mutableStateOf(byteArrayOf())
        private set

    operator fun get(id: Int): Emoji? = emojiMap[id]

    fun MutableList<EmojiClassification>.initializeStatic() {
        add(EmojiClassification("经典", buildList {
            add(Emoji.Static(STATIC_LOGO, Res.drawable.img_logo))
            val drawables = Res.allDrawableResources
            for (index in STATIC_CLASSICS_BEGIN .. STATIC_CLASSICS_END) add(Emoji.Static(index, drawables["emoji$index"]!!))
        }))
    }

    fun MutableList<EmojiClassification>.initializeDynamic() {
        add(EmojiClassification("动图", buildList {
            val drawables = Res.allDrawableResources
            for (index in DYNAMIC_CLASSICS_BEGIN .. DYNAMIC_CLASSICS_END) add(Emoji.Dynamic(index, drawables["emoji$index"]!!))
        }))
    }

    fun MutableList<EmojiClassification>.initializeLottie() {
        add(EmojiClassification("黄脸", buildList {
            val drawables = Res.allDrawableResources
            for (index in LOTTIE_QQ_BEGIN .. LOTTIE_QQ_END) add(Emoji.Lottie(index, drawables["emoji$index"]!!))
        }))
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
        lottieMap = Res.readBytes("files/emoji.lottie")
    }
}