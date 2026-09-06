package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.extension.toJsonString
import love.yinlin.reflect.metaClassName

@Stable
@PublishedApi
internal class Route @PublishedApi internal constructor(private val screenKey: String) {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val KeyMap = mutableMapOf<String, String>()

        @OptIn(CompatibleRachelApi::class)
        inline fun <reified S : BasicScreen> key(): String = metaClassName<S>()

        inline operator fun <reified S : BasicScreen> invoke(): Route = Route(key<S>())

        @PublishedApi
        internal fun find(keyString: String): Route = Route(KeyMap[keyString] ?: keyString)

        internal fun parse(route: String): Triple<String, String, String> {
            val index1 = route.indexOf('|')
            val index2 = route.indexOf('?', index1 + 1)
            val screenName = route.substring(0, index1)
            val uniqueId = route.substring(index1 + 1, index2)
            val args = route.substring(index2 + 1)
            return Triple(screenName, uniqueId, args)
        }
    }

    @PublishedApi
    internal val items = mutableListOf<String>()

    @PublishedApi
    internal fun build(): String = buildString {
        append(screenKey)
        append('|')
        append(ScreenManager.useScreenUniqueId())
        append('?')
        items.joinTo(this, separator = ",", prefix = "[", postfix = "]")
    }

    inline fun <reified A> arg(value: A): Route {
        items += value.toJsonString()
        return this
    }
}