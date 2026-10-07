package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

@Stable
class ScreenRegistry private constructor(builder: Builder) {
    private val factories = builder.factories.toMap()
    private val keyBindings = builder.keyBindings.toMap()
    private val factory404 = builder.factory404

    @OptIn(CompatibleRachelApi::class)
    class Builder internal constructor() {
        internal val factories: MutableMap<String, (ScreenArgs) -> ScreenModel> = mutableMapOf()
        internal val keyBindings: MutableMap<String, String> = mutableMapOf()
        internal var factory404: () -> ScreenModel = ::Screen404

        fun screen404(factory: () -> ScreenModel) { factory404 = factory }

        @PublishedApi
        internal fun register(type: String, argumentCount: Int, key: String? = null, factory: (ScreenArgs) -> ScreenModel) {
            require(type !in factories) { "The screen type is already registered: $type." }
            if (key != null) {
                require(key.isNotBlank()) { "The screen key cannot be blank." }
                require(key !in keyBindings) { "The screen key is already bound: $key." }
            }
            factories[type] = { args ->
                require(args.size == argumentCount) { "The screen type $type expects $argumentCount arguments, but received ${args.size}." }
                factory(args)
            }
            if (key != null) keyBindings[key] = type
        }

        inline fun <reified S : ScreenModel> screen(crossinline factory: () -> S, key: String? = null) =
            register(metaClassName<S>(), 0, key) { factory() }

        inline fun <reified S : ScreenModel, reified A1> screen(crossinline factory: (A1) -> S, key: String? = null) =
            register(metaClassName<S>(), 1, key) { args -> factory(args[0]) }

        inline fun <reified S : ScreenModel, reified A1, reified A2> screen(crossinline factory: (A1, A2) -> S, key: String? = null) =
            register(metaClassName<S>(), 2, key) { args -> factory(args[0], args[1]) }

        inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3> screen(crossinline factory: (A1, A2, A3) -> S, key: String? = null) =
            register(metaClassName<S>(), 3, key) { args -> factory(args[0], args[1], args[2]) }

        inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3, reified A4> screen(crossinline factory: (A1, A2, A3, A4) -> S, key: String? = null) =
            register(metaClassName<S>(), 4, key) { args -> factory(args[0], args[1], args[2], args[3]) }

        inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3, reified A4, reified A5> screen(crossinline factory: (A1, A2, A3, A4, A5) -> S, key: String? = null) =
            register(metaClassName<S>(), 5, key) { args -> factory(args[0], args[1], args[2], args[3], args[4]) }

        inline fun <reified S : ScreenModel> screen(crossinline factory: () -> S) =
            screen(factory, key = null)

        inline fun <reified S : ScreenModel, reified A1> screen(crossinline factory: (A1) -> S) =
            screen(factory, key = null)

        inline fun <reified S : ScreenModel, reified A1, reified A2> screen(crossinline factory: (A1, A2) -> S) =
            screen(factory, key = null)

        inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3> screen(crossinline factory: (A1, A2, A3) -> S) =
            screen(factory, key = null)

        inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3, reified A4> screen(crossinline factory: (A1, A2, A3, A4) -> S) =
            screen(factory, key = null)

        inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3, reified A4, reified A5> screen(crossinline factory: (A1, A2, A3, A4, A5) -> S) =
            screen(factory, key = null)
    }

    internal constructor(builder: Builder.() -> Unit) : this(Builder().apply(builder))

    internal operator fun contains(type: String): Boolean = type in factories

    internal fun resolveKey(key: String): String? = keyBindings[key]

    internal fun create404(): ScreenModel = factory404()

    internal fun create(key: ScreenKey): ScreenModel = if (key.isUnboundKey) create404() else factories[key.type]?.invoke(key.args) ?: create404()
}
