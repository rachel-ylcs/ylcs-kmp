package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaIsAnonymousClass
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class StartupDelegate<S : Startup>(
    type: StartupType,
    val priority: Int,
    name: String? = null,
    private val factory: (PlatformContextProvider) -> S,
    args: Array<Any?>,
) : ReadOnlyProperty<Any?, Startup> {
    companion object {
        const val HIGH10 = 1000
        const val HIGH9 = 900
        const val HIGH8 = 800
        const val HIGH7 = 700
        const val HIGH6 = 600
        const val HIGH5 = 500
        const val HIGH4 = 400
        const val HIGH3 = 300
        const val HIGH2 = 200
        const val HIGH1 = 100
        const val DEFAULT = 0
        const val LOW1 = -100
        const val LOW2 = -200
        const val LOW3 = -300
        const val LOW4 = -400
        const val LOW5 = -500
        const val LOW6 = -600
        const val LOW7 = -700
        const val LOW8 = -800
        const val LOW9 = -900
        const val LOW10 = -1000
    }

    private val startupArgs = StartupArgs(args)
    private lateinit var startup: S
    private var serviceName: String? = name

    override fun getValue(thisRef: Any?, property: KProperty<*>): S {
        if (serviceName == null) serviceName = property.name.ifEmpty { null }
        return startup
    }

    @PublishedApi
    internal val unsafeStartup: S? get() = if (::startup.isInitialized) startup else null

    val isSync: Boolean = type == StartupType.Sync
    val isAsync: Boolean = type == StartupType.Async

    @OptIn(CompatibleRachelApi::class)
    override fun toString(): String {
        val serviceClassName = when {
            !::startup.isInitialized -> "Uninitialized"
            !startup.metaIsAnonymousClass -> startup.toString()
            else -> "Anonymous"
        }
        return "$serviceClassName-${serviceName ?: "unnamed"}"
    }

    fun createStartup(context: PlatformContextProvider) {
        startup = factory(context)
    }

    fun initStartup(scope: CoroutineScope) {
        startup.init(scope, startupArgs)
    }

    suspend fun CoroutineScope.initStartup() {
        with(startup) { init(startupArgs) }
    }

    suspend fun CoroutineScope.initStartupLater() {
        with(startup) { initLater(startupArgs) }
    }

    fun destroyStartup() {
        startup.destroyBefore(startupArgs)
    }

    fun destroyStartupBefore() {
        startup.destroy(startupArgs)
    }
}