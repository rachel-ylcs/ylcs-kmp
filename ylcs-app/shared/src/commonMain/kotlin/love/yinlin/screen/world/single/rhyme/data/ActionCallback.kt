package love.yinlin.screen.world.single.rhyme.data

import androidx.compose.runtime.Stable
import love.yinlin.screen.world.single.rhyme.RhymeSound

@Stable
interface ActionCallback {
    fun updateResult(result: ActionResult, scoreRatio: Float = 1f) // 处理音符结果
    fun playSound(type: RhymeSound) // 播放音效
}