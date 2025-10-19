package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class PlatformMusicType {
    QQMusic, NetEaseCloud, Kugou;

    val description: String get() = when (this) {
        QQMusic -> "QQ音乐"
        NetEaseCloud -> "网易云音乐"
        Kugou -> "酷狗音乐"
    }

    val prefix: String get() = when (this) {
        QQMusic -> "QM"
        NetEaseCloud -> "NEC"
        Kugou -> "KG"
    }
}