package love.yinlin.compose.game.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastMapIndexed
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.visible.Block
import love.yinlin.compose.game.visible.FixedSlurBlock
import love.yinlin.compose.game.visible.NoteBlock
import love.yinlin.compose.game.visible.OffsetSlurBlock
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLyricsConfig

// 地图生成器
class BlockMapGenerator private constructor(
    private val blockDimension: Float,
    lyricsConfig: RhymeLyricsConfig,
    playConfig: RhymePlayConfig
) {
    private val lyrics = lyricsConfig.lyrics
    private val audioOffset = lyricsConfig.offset
    private val difficulty = playConfig.difficulty

    private val occupiedSet = mutableSetOf<Offset>()
    private val result = mutableListOf<List<Offset>>()

    private fun isPosValid(pos: Offset, prevBlock: Offset): Boolean {
        if (occupiedSet.contains(pos)) return false

        // 检查上下左右四个相邻 block
        val neighbors = listOf(
            Offset(pos.x + blockDimension, pos.y),
            Offset(pos.x - blockDimension, pos.y),
            Offset(pos.x, pos.y + blockDimension),
            Offset(pos.x, pos.y - blockDimension)
        )

        for (neighbor in neighbors) {
            // 如果邻居被占用，它必须是紧挨着的上一个 block
            // 这样能保证两条平行路之间至少隔着一个 block 的空隙
            if (occupiedSet.contains(neighbor) && neighbor != prevBlock) return false
        }
        return true
    }

    // DFS 回溯
    private fun solve(segmentIndex: Int, lastX: Float, lastY: Float, dx: Float, dy: Float): Boolean {
        if (segmentIndex == lyrics.size) return true
        val line = lyrics[segmentIndex]
        val segmentData = line.theme

        // 尝试方向, 初始下转，贪心优先左转，尝试右转
        val directions = if (segmentIndex == 0) listOf(Offset(dx, dy)) else listOf(Offset(-dy, dx), Offset(dy, -dx))

        for ((currentDx, currentDy) in directions) {
            val currentSegmentPoints = mutableListOf<Offset>()
            var tempX = lastX
            var tempY = lastY
            var canPlace = true

            // 尝试放置当前 segment 所有 block
            for (_ in segmentData) {
                val nextX = tempX + currentDx
                val nextY = tempY + currentDy
                val nextBlock = Offset(nextX, nextY)

                // 间隙检测：不重叠，且不与除“上一个 Block”外的其他 Block 接触
                if (isPosValid(nextBlock, Offset(tempX, tempY))) {
                    currentSegmentPoints += nextBlock
                    tempX = nextX
                    tempY = nextY
                } else {
                    canPlace = false
                    break
                }
            }

            if (canPlace) {
                // 确认放置
                occupiedSet += currentSegmentPoints
                result += currentSegmentPoints

                // 递归
                if (solve(segmentIndex + 1, tempX, tempY, currentDx, currentDy)) return true

                // 回溯
                result.removeAt(result.size - 1)
                occupiedSet -= currentSegmentPoints.toSet()
            }
        }

        return false
    }

    // 地图生成算法 - 右转优先螺旋
    fun generate(): List<Block<out BlockStatus>> {
        // 生成地图位置
        val blockPositionMap = if (solve(0, -blockDimension, 0f, blockDimension, 0f)) result else emptyList()
        var rawIndex = 0

        // 存储音游配置
        return blockPositionMap.fastMapIndexed { segmentIndex, segments ->
            // 基础属性
            val line = lyrics[segmentIndex]
            val theme = line.theme
            val lastAction = theme.last()
            val lineStart = line.start + audioOffset // 偏移补偿
            val lineEnd = lineStart + lastAction.end

            // 转角
            val cornerPrevBlock = segments.last()
            val cornerNextBlock = blockPositionMap.getOrNull(segmentIndex + 1)?.firstOrNull()
            val corner = when {
                cornerNextBlock == null -> null
                cornerNextBlock.y > cornerPrevBlock.y -> BlockCorner.TopRight
                cornerNextBlock.x < cornerPrevBlock.x -> BlockCorner.BottomRight
                cornerNextBlock.y < cornerPrevBlock.y -> BlockCorner.BottomLeft
                cornerNextBlock.x > cornerPrevBlock.x -> BlockCorner.TopLeft
                else -> null
            }

            // 行属性
            val blockLine = BlockLine(segmentIndex, corner, line.text, lineStart, lineEnd)

            // 遍历方块
            segments.fastMapIndexed { i, pos ->
                val action = theme[i]
                val start = (theme.getOrNull(i - 1)?.end ?: 0) + lineStart
                val end = action.end + lineStart
                when (action) {
                    // 单音
                    is RhymeAction.Note -> NoteBlock(
                        position = pos,
                        line = blockLine,
                        time = NoteBlock.buildTime(difficulty, start),
                        rawIndex = rawIndex++,
                        lineIndex = i,
                        rhymeAction = action
                    )
                    is RhymeAction.Slur -> {
                        val first = action.scale.first()
                        // 延音
                        if (action.scale.fastAll { it == first }) FixedSlurBlock(
                            position = pos,
                            line = blockLine,
                            time = FixedSlurBlock.buildTime(difficulty, start, end),
                            rawIndex = rawIndex++,
                            lineIndex = i,
                            rhymeAction = action
                        )
                        // 连音
                        else OffsetSlurBlock(
                            position = pos,
                            line = blockLine,
                            time = OffsetSlurBlock.buildTime(difficulty, start, end),
                            rawIndex = rawIndex++,
                            lineIndex = i,
                            rhymeAction = action
                        )
                    }
                }
            }
        }.flatten()
    }

    companion object {
        fun generate(blockDimension: Float, lyricsConfig: RhymeLyricsConfig, playConfig: RhymePlayConfig): List<Block<out BlockStatus>> =
            BlockMapGenerator(blockDimension, lyricsConfig, playConfig).generate()
    }
}