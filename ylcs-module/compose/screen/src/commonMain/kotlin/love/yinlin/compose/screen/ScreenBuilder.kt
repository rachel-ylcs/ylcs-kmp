package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Stable
class ScreenBuilder(
    val builder: NavGraphBuilder,
    val manager: ScreenManager
) {
    @JvmName("type1")
    inline fun <reified T> type() = mapOf(typeOf<T>() to getNavType<T>())
    @JvmName("type2")
    inline fun <reified T1, reified T2> type() = mapOf(typeOf<T1>() to getNavType<T1>(), typeOf<T2>() to getNavType<T2>())

    inline fun <reified A> BasicScreen<A>.registerAndLaunch(backStackEntry: NavBackStackEntry): BasicScreen<A> {
        manager.registerScreen(this, backStackEntry.id)
        launch { initialize() }
        return this
    }

    inline fun <reified S : BasicScreen<Unit>> screen(crossinline factory: (ScreenManager) -> S) {
        builder.composable(route = route<S>()) { backStackEntry ->
            val screen = viewModel(key = backStackEntry.id) {
                factory(manager).registerAndLaunch(backStackEntry)
            }
            screen.ComposedUI()
        }
    }

    inline fun <reified A : Any> screen(
        crossinline factory: (ScreenManager, A) -> BasicScreen<A>,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap()
    ) {
        builder.composable<A>(typeMap = typeMap) {  backStackEntry ->
            val screen = viewModel(key = backStackEntry.id) {
                factory(manager, backStackEntry.toRoute<A>()).registerAndLaunch(backStackEntry)
            }
            screen.ComposedUI()
        }
    }
}