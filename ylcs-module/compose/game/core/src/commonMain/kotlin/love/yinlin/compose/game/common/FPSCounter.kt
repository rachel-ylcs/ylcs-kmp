package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Stable
class FPSCounter(@PublishedApi internal val duration: Int = 1000) {
    @PublishedApi internal var counter: Int = 0
    @PublishedApi internal var accumulatedTime: Int = 0
    @PublishedApi internal var fps: Int = 0

    inline fun update(tick: Int, block: (Int) -> Unit) {
        ++counter
        accumulatedTime += tick
        if (accumulatedTime >= duration) {
            fps = counter * 1000 / duration
            counter = 0
            accumulatedTime = 0
            block(fps)
        }
    }
}