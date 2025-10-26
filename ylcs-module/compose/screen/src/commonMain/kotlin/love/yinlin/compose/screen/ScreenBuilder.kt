package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Stable
class ScreenBuilder(
    val builder: NavGraphBuilder,
    val manager: ScreenManager
) {
    inline fun <reified T> type() = typeOf<T>() to getNavType<T>()
    inline fun <reified T> listType() = typeOf<List<T>>() to getNavType<List<T>>()
    inline fun <reified K, reified V> mapType() = typeOf<Map<K, V>>() to getNavType<Map<K, V>>()

    inline fun <reified A> BasicScreen<A>.registerAndLaunch(backStackEntry: NavBackStackEntry): BasicScreen<A> {
        manager.registerScreen(this, backStackEntry.id)
        launch { initialize() }
        return this
    }

    inline fun <reified S : BasicScreen<Unit>> screen(crossinline factory: (ScreenManager) -> S) {
        builder.composable(route = route<S>()) { backStackEntry ->
            val screen = viewModel {
                factory(manager).registerAndLaunch(backStackEntry)
            }
            screen.ComposedUI()
        }
    }

    inline fun <reified A : Any> screen(
        crossinline factory: (ScreenManager, A) -> BasicScreen<A>,
        vararg types: Pair<KType, @JvmSuppressWildcards NavType<*>>
    ) {
        builder.composable<A>(typeMap = mapOf(*types)) {  backStackEntry ->
            val screen = viewModel {
                factory(manager, backStackEntry.toRoute<A>()).registerAndLaunch(backStackEntry)
            }
            screen.ComposedUI()
        }
    }
}