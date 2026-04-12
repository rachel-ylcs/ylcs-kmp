package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import kotlin.math.roundToInt

@Stable
data class BlockTime(
    // 各时间依次递增
    val rawAppearance: Long, // 出现点 Status -> Prepare
    val rawInteract: Long, // miss起始/可交互点
    val badStart: Int, // bad起始
    val goodStart: Int, // good起始
    val perfectStart: Int, // perfect起始
    val standard: Int, // 发声中心点 Status -> Land
    val perfectEnd: Int, // perfect结束
    val goodEnd: Int, // good结束
    val badEnd: Int, // bad结束
) {
    companion object {
        fun build(rule: DifficultyResultRule, start: Long, end: Long): BlockTime {
            val prepare = rule.prepareTime
            val duration = (end - start).toInt()
            val rawInteract = start - duration
            return BlockTime(
                rawAppearance = rawInteract - prepare,
                rawInteract = rawInteract,
                badStart = prepare,
                goodStart = (prepare + duration * (1 - rule.goodRatio)).roundToInt(),
                perfectStart = (prepare + duration * (1 - rule.perfectRatio)).roundToInt(),
                standard = prepare + duration,
                perfectEnd = (prepare + duration * (1 + rule.perfectRatio)).roundToInt(),
                goodEnd = (prepare + duration * (1 + rule.goodRatio)).roundToInt(),
                badEnd = prepare + duration * 2
            )
        }
    }
}