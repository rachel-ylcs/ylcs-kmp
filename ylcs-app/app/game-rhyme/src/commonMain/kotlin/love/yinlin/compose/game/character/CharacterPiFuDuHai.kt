package love.yinlin.compose.game.character

import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.CharacterMetadata

class CharacterPiFuDuHai : Character() {
    override val info: CharacterInfo = CharacterInfo.PiFuDuHai

    val range = (info.metadata["range"] as CharacterMetadata.MPercent).value
}