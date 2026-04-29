package love.yinlin.compose.game.character

import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.data.RhymeSkillResult
import love.yinlin.compose.game.visible.Block
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.CharacterMetadata
import kotlin.random.Random

class CharacterBuLi : Character() {
    override val info: CharacterInfo = CharacterInfo.BuLi

    private val probability = (info.metadata["probability"] as CharacterMetadata.MPercent).value

    private var count: Int = 0

    override fun modifyResult(musicInfo: MusicInfo, config: RhymePlayConfig, block: Block<*>, result: BlockResult): RhymeSkillResult {
        // 有概率BAD结算不重置连击
        return if (result == BlockResult.BAD) {
            val active = Random.nextFloat() < probability
            if (active) showText = "+${++count}"
            RhymeSkillResult(result, active, active)
        } else RhymeSkillResult(result)
    }
}