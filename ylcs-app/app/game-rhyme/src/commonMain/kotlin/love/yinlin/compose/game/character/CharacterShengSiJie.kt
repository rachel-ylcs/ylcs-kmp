package love.yinlin.compose.game.character

import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.data.RhymeSkillResult
import love.yinlin.compose.game.visible.Block
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.CharacterMetadata

class CharacterShengSiJie : Character() {
    override val info: CharacterInfo = CharacterInfo.ShengSiJie

    private val maxCount = (info.metadata["maxCount"] as CharacterMetadata.MInt).value

    private var count: Int = 0

    override fun modifyResult(musicInfo: MusicInfo, config: RhymePlayConfig, block: Block<*>, result: BlockResult): RhymeSkillResult {
        return if (result == BlockResult.MISS && count < maxCount) {
            showText = "${++count} / $maxCount"
            RhymeSkillResult(BlockResult.GOOD, true)
        } else RhymeSkillResult(result)
    }
}