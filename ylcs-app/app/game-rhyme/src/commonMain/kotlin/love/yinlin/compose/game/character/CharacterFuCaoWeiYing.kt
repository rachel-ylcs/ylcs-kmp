package love.yinlin.compose.game.character

import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.data.RhymeSkillResult
import love.yinlin.compose.game.visible.Block
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.CharacterMetadata

class CharacterFuCaoWeiYing : Character() {
    override val info: CharacterInfo = CharacterInfo.FuCaoWeiYing

    private val maxGoodCount = (info.metadata["maxGoodCount"] as CharacterMetadata.MInt).value

    private var goodCount: Int = 0

    override fun modifyResult(musicInfo: MusicInfo, config: RhymePlayConfig, block: Block<*>, result: BlockResult): RhymeSkillResult {
        return if (result == BlockResult.GOOD) {
            // 检查是否超过阈值
            val newResult = if (++goodCount == maxGoodCount) {
                // 转化为PERFECT
                goodCount = 0
                BlockResult.PERFECT
            } else result
            showText = "$goodCount / $maxGoodCount"
            RhymeSkillResult(newResult, newResult == BlockResult.PERFECT, dirty = true)
        } else RhymeSkillResult(result)
    }
}