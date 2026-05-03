package love.yinlin.compose.game.character

import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.CharacterMetadata

class CharacterLiuGuangJi : Character() {
    override val info: CharacterInfo = CharacterInfo.LiuGuangJi

    val range = (info.metadata["range"] as CharacterMetadata.MPercent).value
}