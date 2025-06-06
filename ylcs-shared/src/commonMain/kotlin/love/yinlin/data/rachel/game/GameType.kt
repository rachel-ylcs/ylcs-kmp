package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable

@Stable
enum class GameType(val title: String) {
    RANK("排位"),
    SPEED("竞速")
}