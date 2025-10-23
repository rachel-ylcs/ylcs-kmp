package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

@Stable
class ScreenManager(
    val navController: NavHostController
) {
    @Stable
    data class ScreenInfo(
        val screen: BasicScreen<*>,
        val id: String
    )

    val infoMap = mutableMapOf<String, MutableList<ScreenInfo>>()
    val idMap = mutableMapOf<String, BasicScreen<*>>()

    fun registerScreen(screen: BasicScreen<*>, id: String) {
        val key = screen::class.qualifiedName!!
        val value = infoMap[key]
        val info = ScreenInfo(screen, id)
        if (value != null) value += info
        else infoMap[key] = mutableListOf(info)
        idMap[id] = screen
    }

    fun unregisterScreen(screen: BasicScreen<*>) {
        val key = screen::class.qualifiedName!!
        val value = infoMap[key]
        if (value != null) {
            val index = value.indexOfFirst { it.screen == screen }
            if (index != -1) {
                val id = value[index].id
                idMap.remove(id)
                value.removeAt(index)
                if (value.isEmpty()) infoMap.remove(key)
            }
        }
    }

    val topScreen: BasicScreen<*> get() = idMap[navController.currentBackStackEntry!!.id]!!

    inline fun <reified S : BasicScreen<*>> forScreen(block: S.() -> Unit) {
        val key = S::class.qualifiedName!!
        val value = infoMap[key]
        if (value != null) {
            for ((screen, _) in value) {
                if (screen is S) screen.block()
            }
        }
    }

    inline fun <reified T : Any> navigate(route: T, options: NavOptions? = null, extras: Navigator.Extras? = null) = navController.navigate(route, options, extras)

    inline fun <reified T : BasicScreen<Unit>> navigate(options: NavOptions? = null, extras: Navigator.Extras? = null) = navController.navigate(route<T>(), options, extras)

    fun pop() {
        if (navController.previousBackStackEntry != null) navController.popBackStack()
    }
}