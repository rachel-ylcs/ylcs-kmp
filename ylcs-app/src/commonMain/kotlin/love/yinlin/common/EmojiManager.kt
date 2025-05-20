package love.yinlin.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.resources.*
import org.jetbrains.compose.resources.DrawableResource

@Stable
object EmojiManager {
    // 1 老实小狗
    // 2 瞪眼企鹅

    val emojiMap: Map<Int, DrawableResource> = mapOf(
        1 to Res.drawable.emoji1,
        2 to Res.drawable.emoji2
    )

    const val LOTTIE_BEGIN = 10001
    const val LOTTIE_END = 10017
    // 10000 - 随地大小哭

    var lottieMap: Map<Int, String> by mutableStateOf(emptyMap())

    suspend fun initialize() {
        lottieMap = buildMap {
            for (i in LOTTIE_BEGIN .. LOTTIE_END) {
                put(i, Res.readBytes("files/emoji/$i.json").decodeToString())
            }
        }
    }
}