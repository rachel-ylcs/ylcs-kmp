package love.yinlin.compose.game.traits

interface PreTransform {
    val preTransform: List<Transform> get() = emptyList()
}