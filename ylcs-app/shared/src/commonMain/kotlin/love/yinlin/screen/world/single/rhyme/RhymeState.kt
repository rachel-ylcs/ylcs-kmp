package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
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
    data class Settling(val result: RhymeResult) : GameState // 结算
}

// 游戏锁状态
@Stable
sealed interface GameLockState {
    @Stable
    data object Normal : GameLockState // 正常
    @Stable
    data object PortraitLock : GameLockState // 竖屏锁
    @Stable
    data object Pause : GameLockState // 暂停
    @Stable
    class Resume(t: Int) : GameLockState { // 恢复准备
        var time by mutableIntStateOf(t)

        val timeString by derivedStateOf { time.toString() }
    }
}