package love.yinlin.compose.game.character

import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.data.RhymeSkillResult
import love.yinlin.compose.game.visible.Block
import love.yinlin.compose.game.visible.ContinuedBlock
import love.yinlin.compose.game.visible.MultipleBlock
import love.yinlin.compose.game.visible.NoteBlock
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.rachel.rhyme.CharacterInfo

class CharacterQiXi : Character() {
    override val info: CharacterInfo = CharacterInfo.QiXi

    private var count: Int = 0

    override fun modifyResult(musicInfo: MusicInfo, config: RhymePlayConfig, block: Block<*>, result: BlockResult): RhymeSkillResult {
        if (result == BlockResult.PERFECT) return RhymeSkillResult(result) // 已经是Perfect不管
        val has4 = when (block) {
            is NoteBlock -> block.scaleIndex == 7
            is ContinuedBlock -> block.scaleIndex == 7
            is MultipleBlock -> 7 in block.scaleIndexs
        }
        return if (has4) {
            showText = "+${++count}"
            RhymeSkillResult(BlockResult.PERFECT, true)
        } else RhymeSkillResult(result)
    }
}