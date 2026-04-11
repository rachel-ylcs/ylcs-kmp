package love.yinlin.compose.game.common

sealed class BlockStatus {
    data object None : BlockStatus()
    class Prepare(var progress: Float) : BlockStatus()
    class Interact(var progress: Float, var result: BlockResult) : BlockStatus()
    class Release(var tick: Int, var progress: Float, val result: BlockResult) : BlockStatus()
    class Done(val result: BlockResult) : BlockStatus()
}