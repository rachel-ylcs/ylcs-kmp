package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable

@Stable
enum class GameType(val title: String) {
    RANK("排位"),
    EXPLORATION("探索"),
    SPEED("竞速"),
    SINGLE("单机")
}