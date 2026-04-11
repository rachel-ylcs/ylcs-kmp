package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

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
    val dismiss: Int, // 结束点, 并不一定是最大值
) {
    companion object {
        fun build(rule: DifficultyTimeRule, start: Long, end: Long): BlockTime {
            val prepare = rule.prepareTime
            return BlockTime(
                rawAppearance = start - prepare,
                rawInteract = start - rule.badTime,
                badStart = (prepare - rule.badTime).coerceAtLeast(0),
                goodStart = (prepare - rule.goodTime).coerceAtLeast(0),
                perfectStart = (prepare - rule.perfectTime).coerceAtLeast(0),
                standard = prepare.coerceAtLeast(0),
                perfectEnd = (prepare + rule.perfectTime).coerceAtLeast(0),
                goodEnd = (prepare + rule.goodTime).coerceAtLeast(0),
                badEnd = (prepare + rule.badTime).coerceAtLeast(0),
                dismiss = (prepare + end - start).toInt().coerceAtLeast(0)
            )
        }
    }
}