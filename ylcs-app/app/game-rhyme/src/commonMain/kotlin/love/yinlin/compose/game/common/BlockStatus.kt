package love.yinlin.compose.game.common

interface BlockStatus {
    interface Prepare : BlockStatus
    interface Interact : BlockStatus
    interface Release : BlockStatus {
        val duration: Int
        var progress: Float
        var tick: Int
    }
    interface Missing : Release
    interface Done : BlockStatus
}