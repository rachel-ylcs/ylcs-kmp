package love.yinlin.compose.game.character

import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.data.RhymeSkillResult
import love.yinlin.compose.game.visible.Block
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.rachel.rhyme.CharacterInfo

class CharacterDuanWei : Character() {
    override val info: CharacterInfo = CharacterInfo.DuanWei

    private var count: Int = 0

    override fun modifyResult(musicInfo: MusicInfo, config: RhymePlayConfig, block: Block<*>, result: BlockResult): RhymeSkillResult {
        // 检查是否达到断尾时刻
        val line = block.line
        return if (line.index == line.lineCount - 1) {
            val active = result != BlockResult.PERFECT
            if (active) showText = "+${++count}"
            RhymeSkillResult(BlockResult.PERFECT, active)
        } else RhymeSkillResult(result)
    }
}