package love.yinlin.compose.game.character

import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.CharacterMetadata

class CharacterWuNian : Character() {
    override val info: CharacterInfo = CharacterInfo.WuNian

    private val maxCount = (info.metadata["maxCount"] as CharacterMetadata.MInt).value

    private var count: Int = 0

    fun activate(): Boolean {
        if (count >= maxCount) return false
        showText = "${++count} / $maxCount"
        return true
    }
}