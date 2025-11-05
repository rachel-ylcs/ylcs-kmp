package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import love.yinlin.extension.parseJsonValue
import kotlin.jvm.JvmName

@Stable
class ScreenBuilder(
    val builder: NavGraphBuilder,
    val manager: ScreenManager
) {
    inline fun <reified S : BasicScreen> registerAndLaunch(num: Int, crossinline block: (ScreenManager, NavBackStackEntry, Int) -> S) {
        builder.composable(route = Route.build<S>(num)) { backStackEntry ->
            val screen = viewModel {
                block(manager, backStackEntry, num).apply {
                    manager.registerScreen(this, backStackEntry.id)
                    launch { initialize() }
                }
            }
            screen.ComposedUI()
        }
    }

    inline fun <reified S : BasicScreen> screen(crossinline factory: (ScreenManager) -> S) =
        registerAndLaunch(0) { manager, _, _ ->
            factory(manager)
        }

    @JvmName("screen1a")
    inline fun <reified S : BasicScreen, reified A1 : Any> screen(crossinline factory: (ScreenManager, A1) -> S) =
        registerAndLaunch(1) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue()!!)
        }

    @JvmName("screen1n")
    inline fun <reified S : BasicScreen, reified A1> screen(crossinline factory: (ScreenManager, A1?) -> S) =
        registerAndLaunch(1) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue())
        }

    @JvmName("screen2aa")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2 : Any> screen(crossinline factory: (ScreenManager, A1, A2) -> S) =
        registerAndLaunch(2) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue()!!, args[1].parseJsonValue()!!)
        }

    @JvmName("screen2an")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2> screen(crossinline factory: (ScreenManager, A1, A2?) -> S) =
        registerAndLaunch(2) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue()!!, args[1].parseJsonValue())
        }

    @JvmName("screen2na")
    inline fun <reified S : BasicScreen, reified A1, reified A2 : Any> screen(crossinline factory: (ScreenManager, A1?, A2) -> S) =
        registerAndLaunch(2) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue(), args[1].parseJsonValue()!!)
        }

    @JvmName("screen2nn")
    inline fun <reified S : BasicScreen, reified A1, reified A2> screen(crossinline factory: (ScreenManager, A1?, A2?) -> S) =
        registerAndLaunch(2) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue(), args[1].parseJsonValue())
        }

    @JvmName("screen3aaa")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2 : Any, reified A3 : Any> screen(crossinline factory: (ScreenManager, A1, A2, A3) -> S) =
        registerAndLaunch(3) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue()!!, args[1].parseJsonValue()!!, args[2].parseJsonValue()!!)
        }

    @JvmName("screen3aan")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2 : Any, reified A3> screen(crossinline factory: (ScreenManager, A1, A2, A3?) -> S) =
        registerAndLaunch(3) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue()!!, args[1].parseJsonValue()!!, args[2].parseJsonValue())
        }

    @JvmName("screen3ana")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2, reified A3 : Any> screen(crossinline factory: (ScreenManager, A1, A2?, A3) -> S) =
        registerAndLaunch(3) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue()!!, args[1].parseJsonValue(), args[2].parseJsonValue()!!)
        }

    @JvmName("screen3ann")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2, reified A3> screen(crossinline factory: (ScreenManager, A1, A2?, A3?) -> S) =
        registerAndLaunch(3) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue()!!, args[1].parseJsonValue(), args[2].parseJsonValue())
        }

    @JvmName("screen3naa")
    inline fun <reified S : BasicScreen, reified A1, reified A2 : Any, reified A3 : Any> screen(crossinline factory: (ScreenManager, A1?, A2, A3) -> S) =
        registerAndLaunch(3) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue(), args[1].parseJsonValue()!!, args[2].parseJsonValue()!!)
        }

    @JvmName("screen3nan")
    inline fun <reified S : BasicScreen, reified A1, reified A2 : Any, reified A3> screen(crossinline factory: (ScreenManager, A1?, A2, A3?) -> S) =
        registerAndLaunch(3) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue(), args[1].parseJsonValue()!!, args[2].parseJsonValue())
        }

    @JvmName("screen3nna")
    inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3 : Any> screen(crossinline factory: (ScreenManager, A1?, A2?, A3) -> S) =
        registerAndLaunch(3) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue(), args[1].parseJsonValue(), args[2].parseJsonValue()!!)
        }

    @JvmName("screen3nnn")
    inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3> screen(crossinline factory: (ScreenManager, A1?, A2?, A3?) -> S) =
        registerAndLaunch(3) { manager, backStackEntry, num ->
            val args = Route.fetch(num, backStackEntry)
            factory(manager, args[0].parseJsonValue(), args[1].parseJsonValue(), args[2].parseJsonValue())
        }
}