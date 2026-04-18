package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.common.InteractStatus
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.data.music.RhymeAction

@Stable
class NoteBlock(
    position: Offset,
    line: BlockLine,
    override val time: Time,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Note,
) : Block<NoteBlock.Status>(position, line, rawIndex, lineIndex) {
    @Stable
    data class Time(
        override val appearance: Long,
        val perfectStart: Int,
        val goodStart: Int,
        val badStart: Int,
        val missStart: Int
    ) : BlockTime

    interface Status : BlockStatus {
        class Prepare : Status, BlockStatus.Prepare {
            var progress: Float = 0f
        }
        class Interact : Status, BlockStatus.Interact {
            var progress: Float = 0f
            var result: BlockResult = BlockResult.PERFECT
        }
        class Release(val result: BlockResult) : Status, BlockStatus.Release {
            var progress: Float = 0f
            var tick: Int = 0
        }
        class Done(val result: BlockResult) : Status, BlockStatus.Done
    }

    companion object {
        val PrepareDurationMap = mapOf(
            RhymeDifficulty.Easy to 2000,
            RhymeDifficulty.Medium to 1500,
            RhymeDifficulty.Hard to 1250,
            RhymeDifficulty.Extreme to 1000
        )
        val InteractDurationMap = mapOf(
            RhymeDifficulty.Easy to 1000,
            RhymeDifficulty.Medium to 850,
            RhymeDifficulty.Hard to 650,
            RhymeDifficulty.Extreme to 500
        )

        fun buildTime(difficulty: RhymeDifficulty, start: Long): Time {
            val prepare = PrepareDurationMap[difficulty]!!
            val interactDuration = InteractDurationMap[difficulty]!!
            val perfectDuration = (interactDuration * BlockResult.GOOD.ratio).toInt()
            return Time(
                appearance = start - prepare - perfectDuration / 2,
                perfectStart = prepare,
                goodStart = prepare + perfectDuration,
                badStart = prepare + (interactDuration * BlockResult.BAD.ratio).toInt(),
                missStart = prepare + interactDuration
            )
        }
    }

    private val scaleIndex: Int = (rhymeAction.scale - 1) % 7 + 1
    private val scaleLevel: Int = (rhymeAction.scale - 1) / 7
    private val mainColor: Color = ScaleColorList[scaleIndex]

    override fun prepareStatus(): Status = Status.Prepare()

    override fun onInteract(interactStatus: Array<InteractStatus>, currentStatus: BlockStatus.Interact) {
        if (currentStatus !is Status.Interact) return
        // 单击交互只关心按下时刻
        if (interactStatus[scaleIndex] == InteractStatus.Down) {
            val result = currentStatus.result
            blockStatus = Status.Release(result)
            fromMapLayer?.updateResult(result)
        }
    }

    override fun onUpdate(tick: Int) {
        withMapLayer { mapLayer, audioTick -> // 使用音轨刻
            when (val status = blockStatus) {
                null -> return@withMapLayer false // 未出现不处理
                is Status.Prepare -> {
                    if (audioTick >= time.perfectStart) blockStatus = Status.Interact()
                    else status.progress = (audioTick / time.perfectStart.toFloat()).coerceIn(0f, 1f)
                }
                is Status.Interact -> {
                    val progress = ((audioTick - time.perfectStart) / (time.missStart - time.perfectStart).toFloat()).coerceAtLeast(0f)
                    status.progress = progress
                    status.result = when {
                        progress >= BlockResult.MISS.ratio -> { // 错过
                            val blockResult = BlockResult.MISS
                            blockStatus = Status.Release(blockResult)
                            mapLayer.updateResult(blockResult) // 提交分数
                            blockResult
                        }
                        progress >= BlockResult.BAD.ratio -> BlockResult.BAD
                        progress >= BlockResult.GOOD.ratio -> BlockResult.GOOD
                        else -> BlockResult.PERFECT
                    }
                }
                is Status.Release -> {
                    // Release和Done的动画可以根据游戏刻来而不是音轨刻
                    // 以防音符终止了但动画仍需要继续
                    val oldTick = status.tick
                    if (oldTick >= RELEASE_ANIMATION_DURATION) blockStatus = Status.Done(status.result)
                    else {
                        val newTick = oldTick + tick
                        status.tick = newTick
                        val rawProgress = (newTick / RELEASE_ANIMATION_DURATION.toFloat()).coerceIn(0f, 1f) - 1
                        // 用OvershootInterpolator插值
                        status.progress = rawProgress * rawProgress * (3 * rawProgress + 2) + 1
                    }
                }
                is Status.Done -> return@withMapLayer false
            }
            true
        }
    }

    override fun Drawer.onDraw() {
        when (val status = blockStatus) {
            null -> return
            is Status.Prepare -> {
                withBlockScale {
                    drawPrepareBorder(mainColor, status.progress)
                    drawSingleNoteFont(rhymeAction.scale.toInt(), (status.progress * 2).coerceIn(0f, 1f))
                }
            }
            is Status.Interact -> {
                withBlockScale {
                    scale(status.progress, DefaultCenter) { rect(mainColor, DefaultRect) }
                    drawPrepareBorder(mainColor, 1f)
                }
            }
            is Status.Release -> {
                withBlockScale {
                    drawPrepareBorder(mainColor, 1f)
                }
            }
            is Status.Done -> {
                withBlockScale {
                    drawPrepareBorder(mainColor, 1f)
                }
            }
        }
    }
}