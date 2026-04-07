package love.yinlin.compose.game.data

import androidx.compose.runtime.Stable
import love.yinlin.data.music.MusicInfo

@Stable
internal sealed interface RhymeState {
    @Stable
    data object Start : RhymeState // 开始
    @Stable
    data object MusicLibrary : RhymeState // 曲库
    @Stable
    data class Prepare(val info: MusicInfo): RhymeState // 准备
    @Stable
    data class Playing(val info: MusicInfo, val playConfig: RhymePlayConfig) : RhymeState // 游戏中
    @Stable
    data class Settling(val info: MusicInfo, val playConfig: RhymePlayConfig, val result: RhymePlayResult) : RhymeState // 结算
    @Stable
    data object Rank : RhymeState // 排行榜
}