package love.yinlin.compose.game.common

interface BlockStatus {
    interface Prepare : BlockStatus
    interface Interact : BlockStatus
    interface Release : BlockStatus
    interface Done : BlockStatus
}