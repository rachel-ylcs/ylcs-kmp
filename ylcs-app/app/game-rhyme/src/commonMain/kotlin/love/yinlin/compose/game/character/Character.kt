package love.yinlin.compose.game.character

import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.data.RhymeSkillResult
import love.yinlin.compose.game.visible.Block
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.rachel.rhyme.CharacterInfo

sealed class Character {
    abstract val info: CharacterInfo

    open fun modifyResult(musicInfo: MusicInfo, config: RhymePlayConfig, block: Block<*>, result: BlockResult): RhymeSkillResult = RhymeSkillResult(result)

    var showText: String? = null

    companion object {
        val Factory = mapOf(
            CharacterInfo.BuLi to ::CharacterBuLi,
            CharacterInfo.ChiChi to ::CharacterChiChi,
            CharacterInfo.ChuXing to ::CharacterChuXing,
            CharacterInfo.DuanWei to ::CharacterDuanWei,
            CharacterInfo.FuCaoWeiYing to ::CharacterFuCaoWeiYing,
            CharacterInfo.LiDiShiGongFenA to ::CharacterLiDiShiGongFenA,
            CharacterInfo.LiDiShiGongFenB to ::CharacterLiDiShiGongFenB,
            CharacterInfo.PiFuDuHai to ::CharacterPiFuDuHai,
            CharacterInfo.WuJiYa to ::CharacterWuJiYa,
        )
    }
}