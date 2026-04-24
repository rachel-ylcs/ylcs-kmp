package love.yinlin.compose.game.common

interface BlockStatus {
    abstract class Prepare : BlockStatus {
        var progress: Float = 0f
    }
    interface Interact : BlockStatus
    interface Release : BlockStatus {
        val duration: Int
        var progress: Float
        var tick: Int
    }
    interface Missing : Release
    interface Done : BlockStatus
}