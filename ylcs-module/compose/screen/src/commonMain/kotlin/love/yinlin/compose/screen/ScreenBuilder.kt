package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import love.yinlin.extension.parseJsonValue
import kotlin.jvm.JvmName

@Stable
class ScreenBuilder(
    @PublishedApi internal val builder: NavGraphBuilder,
    @PublishedApi internal val manager: ScreenManager
) {
    inline fun <reified T : Any> List<String>.a(index: Int): T = this[index].parseJsonValue()
    inline fun <reified T> List<String>.n(index: Int): T? = this[index].parseJsonValue()

    inline fun <reified S : BasicScreen> registerAndLaunch(num: Int, crossinline block: (args: List<String>) -> S) {
        builder.composable(route = Route.build<S>(num)) { backStackEntry ->
            val screen = viewModel {
                val args = Route.fetch(num, backStackEntry)
                block(args).apply {
                    manager.registerScreen(this, backStackEntry.id)
                    launch { initialize() }
                }
            }
            screen.ComposedUI()
        }
    }

    inline fun <reified S : BasicScreen> screen(crossinline factory: (ScreenManager) -> S) =
        registerAndLaunch(0) { factory(manager) }

    @JvmName("screen1a")
    inline fun <reified S : BasicScreen, reified A1 : Any> screen(crossinline factory: (ScreenManager, A1) -> S) =
        registerAndLaunch(1) { args -> factory(manager, args.a(0)) }

    @JvmName("screen1n")
    inline fun <reified S : BasicScreen, reified A1> screen(crossinline factory: (ScreenManager, A1?) -> S) =
        registerAndLaunch(1) { args -> factory(manager, args.n(0)) }

    @JvmName("screen2aa")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2 : Any> screen(crossinline factory: (ScreenManager, A1, A2) -> S) =
        registerAndLaunch(2) { args -> factory(manager, args.a(0), args.a(1)) }

    @JvmName("screen2an")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2> screen(crossinline factory: (ScreenManager, A1, A2?) -> S) =
        registerAndLaunch(2) { args -> factory(manager, args.a(0), args.n(1)) }

    @JvmName("screen2na")
    inline fun <reified S : BasicScreen, reified A1, reified A2 : Any> screen(crossinline factory: (ScreenManager, A1?, A2) -> S) =
        registerAndLaunch(2) { args -> factory(manager, args.n(0), args.a(1)) }

    @JvmName("screen2nn")
    inline fun <reified S : BasicScreen, reified A1, reified A2> screen(crossinline factory: (ScreenManager, A1?, A2?) -> S) =
        registerAndLaunch(2) { args -> factory(manager, args.n(0), args.n(1)) }

    @JvmName("screen3aaa")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2 : Any, reified A3 : Any> screen(crossinline factory: (ScreenManager, A1, A2, A3) -> S) =
        registerAndLaunch(3) { args -> factory(manager, args.a(0), args.a(1), args.a(2)) }

    @JvmName("screen3aan")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2 : Any, reified A3> screen(crossinline factory: (ScreenManager, A1, A2, A3?) -> S) =
        registerAndLaunch(3) { args -> factory(manager, args.a(0), args.a(1), args.n(2)) }

    @JvmName("screen3ana")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2, reified A3 : Any> screen(crossinline factory: (ScreenManager, A1, A2?, A3) -> S) =
        registerAndLaunch(3) { args -> factory(manager, args.a(0), args.n(1), args.a(2)) }

    @JvmName("screen3ann")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2, reified A3> screen(crossinline factory: (ScreenManager, A1, A2?, A3?) -> S) =
        registerAndLaunch(3) { args -> factory(manager, args.a(0), args.n(1), args.n(2)) }

    @JvmName("screen3naa")
    inline fun <reified S : BasicScreen, reified A1, reified A2 : Any, reified A3 : Any> screen(crossinline factory: (ScreenManager, A1?, A2, A3) -> S) =
        registerAndLaunch(3) { args -> factory(manager, args.n(0), args.a(1), args.a(2)) }

    @JvmName("screen3nan")
    inline fun <reified S : BasicScreen, reified A1, reified A2 : Any, reified A3> screen(crossinline factory: (ScreenManager, A1?, A2, A3?) -> S) =
        registerAndLaunch(3) { args -> factory(manager, args.n(0), args.a(1), args.n(2)) }

    @JvmName("screen3nna")
    inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3 : Any> screen(crossinline factory: (ScreenManager, A1?, A2?, A3) -> S) =
        registerAndLaunch(3) { args -> factory(manager, args.n(0), args.n(1), args.a(2)) }

    @JvmName("screen3nnn")
    inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3> screen(crossinline factory: (ScreenManager, A1?, A2?, A3?) -> S) =
        registerAndLaunch(3) { args -> factory(manager, args.n(0), args.n(1), args.n(2)) }
}