package love.yinlin.compose.game.character

import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.CharacterMetadata

class CharacterChiChi : Character() {
    override val info: CharacterInfo = CharacterInfo.ChiChi

    val range = (info.metadata["range"] as CharacterMetadata.MPercent).value
}