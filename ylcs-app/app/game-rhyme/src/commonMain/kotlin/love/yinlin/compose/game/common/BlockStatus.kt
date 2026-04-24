package love.yinlin.compose.game.common

interface BlockStatus {
    abstract class Prepare : BlockStatus {
        var progress: Float = 0f
    }
    interface Interact : BlockStatus
    abstract class Release : BlockStatus {
        abstract val duration: Int
        var progress: Float = 0f
        var tick: Int = 0
    }
    abstract class Missing : Release()
    interface Done : BlockStatus
}