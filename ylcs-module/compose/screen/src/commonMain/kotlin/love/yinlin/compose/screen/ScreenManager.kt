package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

@Stable
class ScreenManager(
    val navController: NavHostController
) {
    private val vmMap = mutableMapOf<String, MutableList<ViewModel>>()

    fun loadViewModel(vm: ViewModel) {
        val key = vm::class.qualifiedName!!
        val value = vmMap[key]
        if (value != null) value += vm
        else vmMap[key] = mutableListOf(vm)
    }

    fun unloadViewModel(vm: ViewModel) {
        val key = vm::class.qualifiedName!!
        val value = vmMap[key]
        if (value != null) {
            if (value.remove(vm)) {
                if (value.isEmpty()) vmMap.remove(key)
            }
        }
    }

    inline fun <reified T : Any> navigate(route: T, options: NavOptions? = null, extras: Navigator.Extras? = null) = navController.navigate(route, options, extras)

    inline fun <reified T : BasicScreen<Unit>> navigate(options: NavOptions? = null, extras: Navigator.Extras? = null) = navController.navigate(route<T>(), options, extras)

    fun pop() {
        if (navController.previousBackStackEntry != null) navController.popBackStack()
    }
}