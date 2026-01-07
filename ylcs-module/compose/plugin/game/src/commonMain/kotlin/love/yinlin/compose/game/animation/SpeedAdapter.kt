package love.yinlin.compose.game.animation

class SpeedAdapter(private val speed: Float) : FrameAdapter {
    var virtualFrame = -1f

    override fun onFrameUpdate(): Int {
        virtualFrame += speed
        return virtualFrame.toInt()
    }

    override fun onFrameStart() {
        virtualFrame = 0f
    }

    override fun onFrameReset() {
        virtualFrame = -1f
    }
}