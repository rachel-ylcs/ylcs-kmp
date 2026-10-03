package love.yinlin.compose.data

import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.extension.StringSerializer
import love.yinlin.reflect.metaClassName

@OptIn(CompatibleRachelApi::class)
object ItemKeySerializer : StringSerializer<ItemKey>(metaClassName<ItemKey>()) {
    override fun encode(value: ItemKey): String = value.value
    override fun decode(value: String): ItemKey = ItemKey(value)
}