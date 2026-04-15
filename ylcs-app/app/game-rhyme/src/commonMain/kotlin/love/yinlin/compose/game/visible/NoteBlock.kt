package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.data.music.RhymeAction
import kotlin.math.abs

@Stable
class NoteBlock(
    position: Offset,
    line: BlockLine,
    time: BlockTime,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Note,
) : Block(position, line, time, rawIndex, lineIndex) {
    private val scaleIndex: Int = (rhymeAction.scale - 1) % 7 + 1
    private val scaleLevel: Int = (rhymeAction.scale - 1) / 7
    private val mainColor: Color = ScaleColorList[scaleIndex]

    override fun onUpdate(tick: Int) {
        withMapLayer { mapLayer, audioTick -> // 使用音轨刻
            when (val status = blockStatus) {
                is BlockStatus.None -> return@withMapLayer false // 未出现不处理
                is BlockStatus.Prepare -> {
                    if (audioTick >= time.badStart) blockStatus = BlockStatus.Interact(0f, BlockResult.BAD)
                    else status.progress = (audioTick / time.badStart.toFloat()).coerceIn(0f, 1f)
                }
                is BlockStatus.Interact -> {
                    val progress = 1 - (abs(time.standard - audioTick) / (time.standard - time.badStart).toFloat()).coerceIn(0f, 1f)
                    when (status.result) {
                        BlockResult.BAD -> when {
                            audioTick >= time.badEnd -> {
                                blockStatus = BlockStatus.Release(0, 0f, BlockResult.MISS)
                                mapLayer.updateResult(BlockResult.MISS) // 提交分数
                            }
                            audioTick >= time.goodEnd -> status.progress = progress
                            audioTick >= time.goodStart -> status.result = BlockResult.GOOD
                            else -> status.progress = progress
                        }
                        BlockResult.GOOD -> when {
                            audioTick >= time.goodEnd -> status.result = BlockResult.BAD
                            audioTick >= time.perfectEnd -> status.progress = progress
                            audioTick >= time.perfectStart -> status.result = BlockResult.PERFECT
                            else -> status.progress = progress
                        }
                        BlockResult.PERFECT -> when {
                            audioTick >= time.perfectEnd -> status.result = BlockResult.GOOD
                            audioTick >= time.perfectStart -> status.progress = progress
                        }
                        BlockResult.MISS -> { } // 不可能是MISS
                    }
                }
                is BlockStatus.Release -> {
                    // Release和Done的动画可以根据游戏刻来而不是音轨刻
                    // 以防音符终止了但动画仍需要继续
                    val oldTick = status.tick
                    if (oldTick >= RELEASE_ANIMATION_DURATION) blockStatus = BlockStatus.Done(status.result)
                    else {
                        val newTick = oldTick + tick
                        status.tick = newTick
                        val rawProgress = (newTick / RELEASE_ANIMATION_DURATION.toFloat()).coerceIn(0f, 1f) - 1
                        // 用OvershootInterpolator插值
                        status.progress = rawProgress * rawProgress * (3 * rawProgress + 2) + 1
                    }
                }
                is BlockStatus.Done -> { }
            }
            true
        }
    }

    override fun Drawer.onDraw() {
        when (val status = blockStatus) {
            is BlockStatus.None -> return
            is BlockStatus.Prepare -> {
                withBlockScale { drawCommonPrepare(mainColor, status.progress) }
            }
            is BlockStatus.Interact -> {
                withBlockScale {
                    scale(status.progress, DefaultCenter) { rect(mainColor, DefaultRect) }
                    drawCommonPrepare(mainColor, 1f)
                }
            }
            is BlockStatus.Release -> {
                withBlockScale {
                    drawCommonPrepare(mainColor, 1f)
                    scale(status.progress * BLOCK_RESULT_SCALE, DefaultCenter) { drawBlockResult(mainColor, status.result) }
                }
            }
            is BlockStatus.Done -> {
                withBlockScale {
                    drawCommonPrepare(mainColor, 1f)
                    scale(BLOCK_RESULT_SCALE, DefaultCenter) { drawBlockResult(mainColor, status.result) }
                }
            }
        }
    }
}