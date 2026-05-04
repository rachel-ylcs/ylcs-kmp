package love.yinlin.data.music

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import love.yinlin.compose.Colors
import love.yinlin.compose.ui.icon.Icons2

@Stable
@Serializable
enum class PlatformMusicType {
    QQMusic, NetEaseCloud, Kugou, Soda, Migu;

    val description: String get() = when (this) {
        QQMusic -> "QQ音乐"
        NetEaseCloud -> "网易云音乐"
        Kugou -> "酷狗音乐"
        Soda -> "汽水音乐"
        Migu -> "咪咕音乐"
    }

    val prefix: String get() = when (this) {
        QQMusic -> "QM"
        NetEaseCloud -> "NEC"
        Kugou -> "KG"
        Soda -> "QS"
        Migu -> "MG"
    }

    val icon: ImageVector get() = when (this) {
        QQMusic -> Icons2.QQMusic
        NetEaseCloud -> Icons2.NetEaseCloudMusic
        Kugou -> Icons2.KugouMusic
        Soda -> Icons2.SodaMusic
        Migu -> Icons2.MiguMusic
    }

    val color: Color get() = when (this) {
        QQMusic -> Colors.Yellow4
        NetEaseCloud -> Colors.Red4
        Kugou -> Colors.Blue4
        Soda -> Colors.Green4
        Migu -> Colors.Pink4
    }
}
