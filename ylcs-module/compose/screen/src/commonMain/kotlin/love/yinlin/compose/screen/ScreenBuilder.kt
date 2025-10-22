package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewmodel.compose.viewModel
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

    inline fun <reified S : BasicScreen<Unit>> screen(crossinline factory: (ScreenManager) -> S) {
        builder.composable(route = route<S>()) {
            val screen = viewModel {
                factory(manager).also {
                    it.launch { it.initialize() }
                }
            }
            screen.ComposedUI()
        }
    }

    inline fun <reified A : Any> screen(
        crossinline factory: (ScreenManager, A) -> BasicScreen<A>,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap()
    ) {
        builder.composable<A>(typeMap = typeMap) {  backStackEntry ->
            val screen = viewModel {
                factory(manager, backStackEntry.toRoute<A>()).also {
                    it.launch { it.initialize() }
                }
            }
            screen.ComposedUI()
        }
    }
}