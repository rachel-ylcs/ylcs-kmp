package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import love.yinlin.data.music.MusicInfo

@Stable
internal data class RhymeMusic(
    val musicInfo: MusicInfo,
    val enabled: Boolean
)

// 游戏页状态
@Stable
internal sealed interface GameState {
    @Stable
    data object Loading : GameState // 加载中
    @Stable
    data object Start : GameState // 开始
    @Stable
    data object MusicLibrary : GameState // 音乐库
    @Stable
    data class MusicDetails(val entry: RhymeMusic) : GameState // 音乐详情
    @Stable
    data object Playing : GameState // 游戏中
    @Stable
    data object Settling : GameState // 结算
}

// 游戏锁状态
@Stable
internal sealed interface GameLockState {
    @Stable
    data object Normal : GameLockState // 正常
    @Stable
    data object PortraitLock : GameLockState // 竖屏锁
    @Stable
    data object Pause : GameLockState // 暂停
    @Stable
    data class Resume(val time: Int) : GameLockState // 恢复准备
}