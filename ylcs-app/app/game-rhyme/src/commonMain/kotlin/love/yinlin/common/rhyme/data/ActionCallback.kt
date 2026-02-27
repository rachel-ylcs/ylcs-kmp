package love.yinlin.common.rhyme.data

import androidx.compose.runtime.Stable
import love.yinlin.common.rhyme.RhymeSound

@Stable
interface ActionCallback {
    fun updateResult(result: ActionResult, scoreRatio: Float = 1f) // 处理音符结果
    fun playSound(type: RhymeSound) // 播放音效
}