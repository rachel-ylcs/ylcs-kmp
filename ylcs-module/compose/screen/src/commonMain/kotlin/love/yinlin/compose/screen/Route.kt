package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.extension.toJsonString
import love.yinlin.reflect.metaClassName

@Stable
@PublishedApi
internal class Route(private val screenKey: String) {
    companion object {
        const val SCREEN_404 = "404"

        @OptIn(CompatibleRachelApi::class)
        inline fun <reified S : BasicScreen> key(): String = metaClassName<S>()

        inline operator fun <reified S : BasicScreen> invoke(): Route = Route(key<S>())

        fun parse(route: String): Triple<String, String, String> {
            val index1 = route.indexOf('|')
            val index2 = route.indexOf('?', index1 + 1)
            val screenName = route.substring(0, index1)
            val uniqueId = route.substring(index1 + 1, index2)
            val args = route.substring(index2 + 1)
            return Triple(screenName, uniqueId, args)
        }
    }

    val items = mutableListOf<String>()

    fun build(): String = buildString {
        append(screenKey)
        append('|')
        append(ScreenGlobal.ScreenUniqueId++)
        append('?')
        items.joinTo(this, separator = ",", prefix = "[", postfix = "]")
    }

    inline fun <reified A> arg(value: A): Route {
        items += value.toJsonString()
        return this
    }
}