package love.yinlin.compose.game.animation

interface FrameAdapter {
    fun onFrameUpdate(): Int
    fun onFrameStart()
    fun onFrameReset()
}