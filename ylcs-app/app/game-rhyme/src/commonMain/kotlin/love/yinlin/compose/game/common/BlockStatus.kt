package love.yinlin.compose.game.common

sealed interface BlockStatus {
    data object None : BlockStatus
    class Prepare(var tick: Int) : BlockStatus
    class Interact(var tick: Int) : BlockStatus
    class Score(var tick: Int): BlockStatus
    class End(var tick: Int) : BlockStatus
}