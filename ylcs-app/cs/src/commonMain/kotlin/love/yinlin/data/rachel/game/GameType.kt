package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable

@Stable
enum class GameType(val order: Int, val title: String) {
    RANK(1, "排位"),
    EXPLORATION(2, "探索"),
    SPEED(3, "竞速"),
    SINGLE(0, "独立"),
    BATTLE(4, "对战");
}