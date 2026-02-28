package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonArray
import love.yinlin.extension.to

@Stable
@PublishedApi
internal object ScreenGlobal {
    var ScreenUniqueId = 0L

    val VMMap = mutableMapOf<String, BasicScreen>()
}

@PublishedApi
internal inline fun <reified T : Any> JsonArray.a(index: Int): T = this[index].to()
@PublishedApi
internal inline fun <reified T> JsonArray.n(index: Int): T? = this[index].to()