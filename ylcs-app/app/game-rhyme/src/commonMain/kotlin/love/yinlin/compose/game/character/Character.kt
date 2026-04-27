package love.yinlin.compose.game.character

import love.yinlin.data.rachel.rhyme.CharacterInfo

sealed interface Character {
    val info: CharacterInfo

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