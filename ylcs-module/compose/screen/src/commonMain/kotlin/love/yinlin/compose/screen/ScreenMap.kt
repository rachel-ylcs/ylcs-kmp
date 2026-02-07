package love.yinlin.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonArray
import kotlin.jvm.JvmName

@Stable
class ScreenMap @PublishedApi internal constructor() {
    @PublishedApi
    internal val screens = mutableMapOf<String, (JsonArray) -> BasicScreen>()

    var screen404: (@Composable () -> Unit)? = null

    @PublishedApi
    internal inline fun <reified S : BasicScreen> screen(noinline handler: (JsonArray) -> BasicScreen) { screens[Route.key<S>()] = handler }

    @JvmName("screen0")
    inline fun <reified S : BasicScreen> screen(crossinline factory: () -> S) =
        screen<S> { _: JsonArray -> factory() }

    @JvmName("screen1a")
    inline fun <reified S : BasicScreen, reified A1 : Any> screen(crossinline factory: (A1) -> S) =
        screen<S> { args: JsonArray -> factory(args.a(0)) }

    @JvmName("screen1n")
    inline fun <reified S : BasicScreen, reified A1> screen(crossinline factory: (A1?) -> S) =
        screen<S> { args: JsonArray -> factory(args.n(0)) }

    @JvmName("screen2aa")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2 : Any> screen(crossinline factory: (A1, A2) -> S) =
        screen<S> { args: JsonArray -> factory(args.a(0), args.a(1)) }

    @JvmName("screen2an")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2> screen(crossinline factory: (A1, A2?) -> S) =
        screen<S> { args: JsonArray -> factory(args.a(0), args.n(1)) }

    @JvmName("screen2na")
    inline fun <reified S : BasicScreen, reified A1, reified A2 : Any> screen(crossinline factory: (A1?, A2) -> S) =
        screen<S> { args: JsonArray -> factory(args.n(0), args.a(1)) }

    @JvmName("screen2nn")
    inline fun <reified S : BasicScreen, reified A1, reified A2> screen(crossinline factory: (A1?, A2?) -> S) =
        screen<S> { args: JsonArray -> factory(args.n(0), args.n(1)) }

    @JvmName("screen3aaa")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2 : Any, reified A3 : Any> screen(crossinline factory: (A1, A2, A3) -> S) =
        screen<S> { args: JsonArray -> factory(args.a(0), args.a(1), args.a(2)) }

    @JvmName("screen3aan")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2 : Any, reified A3> screen(crossinline factory: (A1, A2, A3?) -> S) =
        screen<S> { args: JsonArray -> factory(args.a(0), args.a(1), args.n(2)) }

    @JvmName("screen3ana")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2, reified A3 : Any> screen(crossinline factory: (A1, A2?, A3) -> S) =
        screen<S> { args: JsonArray -> factory(args.a(0), args.n(1), args.a(2)) }

    @JvmName("screen3ann")
    inline fun <reified S : BasicScreen, reified A1 : Any, reified A2, reified A3> screen(crossinline factory: (A1, A2?, A3?) -> S) =
        screen<S> { args: JsonArray -> factory(args.a(0), args.n(1), args.n(2)) }

    @JvmName("screen3naa")
    inline fun <reified S : BasicScreen, reified A1, reified A2 : Any, reified A3 : Any> screen(crossinline factory: (A1?, A2, A3) -> S) =
        screen<S> { args: JsonArray -> factory(args.n(0), args.a(1), args.a(2)) }

    @JvmName("screen3nan")
    inline fun <reified S : BasicScreen, reified A1, reified A2 : Any, reified A3> screen(crossinline factory: (A1?, A2, A3?) -> S) =
        screen<S> { args: JsonArray -> factory(args.n(0), args.a(1), args.n(2)) }

    @JvmName("screen3nna")
    inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3 : Any> screen(crossinline factory: (A1?, A2?, A3) -> S) =
        screen<S> { args: JsonArray -> factory(args.n(0), args.n(1), args.a(2)) }

    @JvmName("screen3nnn")
    inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3> screen(crossinline factory: (A1?, A2?, A3?) -> S) =
        screen<S> { args: JsonArray -> factory(args.n(0), args.n(1), args.n(2)) }
}