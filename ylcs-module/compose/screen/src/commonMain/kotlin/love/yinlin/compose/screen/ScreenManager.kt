package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import androidx.navigation.NavHostController

@Stable
class ScreenManager(val navController: NavHostController) {
    @Stable
    data class ScreenInfo(
        val screen: BasicScreen,
        val id: String
    )

    val infoMap = mutableMapOf<String, MutableList<ScreenInfo>>()
    val idMap = mutableMapOf<String, BasicScreen>()

    fun registerScreen(screen: BasicScreen, id: String) {
        val key = screen::class.qualifiedName!!
        val value = infoMap[key]
        val info = ScreenInfo(screen, id)
        if (value != null) value += info
        else infoMap[key] = mutableListOf(info)
        idMap[id] = screen
    }

    fun unregisterScreen(screen: BasicScreen) {
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

    val top: BasicScreen get() = idMap[navController.currentBackStackEntry!!.id]!!

    inline fun <reified S : BasicScreen> get(): S = infoMap[S::class.qualifiedName!!]!!.first().screen as S

    inline fun <reified S : BasicScreen> forEach(block: S.() -> Unit) {
        val key = S::class.qualifiedName!!
        val value = infoMap[key]
        if (value != null) {
            for ((screen, _) in value) {
                if (screen is S) screen.block()
            }
        }
    }

    inline fun <reified S : BasicScreen> navigate(s: (ScreenManager) -> S) =
        navController.navigate(route = route<S>().toString())

    inline fun <reified S : BasicScreen, reified A1> navigate(s: (ScreenManager, A1) -> S, arg1: A1) =
        navController.navigate(route = route<S>().arg(arg1).toString())

    inline fun <reified S : BasicScreen, reified A1, reified A2> navigate(s: (ScreenManager, A1, A2) -> S, arg1: A1, arg2: A2) =
        navController.navigate(route = route<S>().arg(arg1).arg(arg2).toString())

    inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3> navigate(s: (ScreenManager, A1, A2, A3) -> S, arg1: A1, arg2: A2, arg3: A3) =
        navController.navigate(route = route<S>().arg(arg1).arg(arg2).arg(arg3).toString())

    fun pop() {
        if (navController.previousBackStackEntry != null) navController.popBackStack()
    }
}