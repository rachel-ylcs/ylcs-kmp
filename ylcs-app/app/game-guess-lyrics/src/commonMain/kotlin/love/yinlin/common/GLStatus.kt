package love.yinlin.common

import androidx.compose.runtime.Stable
import love.yinlin.cs.sockets.LyricsSockets

@Stable
sealed interface GLStatus {
    @Stable
    data object Hall : GLStatus
    @Stable
    data class InviteLoading(val info: LyricsSockets.PlayerInfo, val time: Long) : GLStatus
    @Stable
    data class InvitedLoading(val info: LyricsSockets.PlayerInfo, val time: Long) : GLStatus
    @Stable
    data class Preparing(val info1: LyricsSockets.PlayerInfo, val info2: LyricsSockets.PlayerInfo, val time: Long) : GLStatus
    @Stable
    data class Playing(val info1: LyricsSockets.PlayerInfo, val info2: LyricsSockets.PlayerInfo, val time: Long, val questions: List<Pair<String, Int>>, val answers: List<String?>, val count1: Int, val count2: Int) : GLStatus
    @Stable
    data class Waiting(val info: LyricsSockets.PlayerInfo) : GLStatus
    @Stable
    data class Settling(val result1: LyricsSockets.GameResult, val result2: LyricsSockets.GameResult) : GLStatus
}