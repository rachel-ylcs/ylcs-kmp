package love.yinlin.compose.game.data

import androidx.compose.runtime.Stable
import love.yinlin.data.music.MusicInfo

@Stable
internal sealed interface GameState {
    @Stable
    data object Start : GameState // 开始
    @Stable
    data object MusicLibrary : GameState // 曲库
    @Stable
    data class Prepare(val info: MusicInfo): GameState // 准备
    @Stable
    data object Playing : GameState // 游戏中
    @Stable
    data object Rank : GameState // 排行榜
}