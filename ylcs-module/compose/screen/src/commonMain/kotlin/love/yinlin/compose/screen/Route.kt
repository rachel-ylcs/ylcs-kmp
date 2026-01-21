package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import androidx.navigation.NavBackStackEntry
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.uri.Uri
import love.yinlin.extension.toJsonString
import love.yinlin.reflect.metaClassName

@Stable
class Route(name: String) {
    val mBuilder = StringBuilder().append(name)

    inline fun <reified A> arg(v: A): Route {
        mBuilder.append('|')
        mBuilder.append(Uri.encodeUri(v.toJsonString()))
        return this
    }

    override fun toString(): String = mBuilder.toString()

    companion object {
        fun argName(index: Int) = "arg$index"

        @OptIn(CompatibleRachelApi::class)
        inline fun <reified S : BasicScreen> build(num: Int): String = buildString {
            append(metaClassName<S>())
            repeat(num) { index ->
                append("|{${argName(index)}}")
            }
        }

        fun fetch(num: Int, backStackEntry: NavBackStackEntry): List<String> {
            val handle = backStackEntry.savedStateHandle
            return List(num) { index -> Uri.decodeUri(handle.get<String>(argName(index))!!) }
        }
    }
}

@OptIn(CompatibleRachelApi::class)
inline fun <reified S : BasicScreen> route(): Route = Route(metaClassName<S>())