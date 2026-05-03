package love.yinlin.compose.game.character

import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.data.RhymeSkillResult
import love.yinlin.compose.game.visible.Block
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.rachel.rhyme.CharacterInfo

class CharacterLiDiShiGongFenA : Character() {
    override val info: CharacterInfo = CharacterInfo.LiDiShiGongFenA

    private var count: Int = 0

    override fun modifyResult(musicInfo: MusicInfo, config: RhymePlayConfig, block: Block<*>, result: BlockResult): RhymeSkillResult {
        // 将所有BAD转化为GOOD
        return if (result == BlockResult.BAD) {
            showText = "+${++count}"
            RhymeSkillResult(BlockResult.GOOD, true)
        } else RhymeSkillResult(result)
    }
}