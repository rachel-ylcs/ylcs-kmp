package love.yinlin.service

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class StartupDelegate<S : Startup> internal constructor(
    val privilege: StartupPrivilege,
    val type: StartupType,
    private val factory: () -> S,
    args: Array<Any?>,
    val priority: Int,
) : ReadOnlyProperty<Any?, Startup> {
    constructor(type: StartupType, factory: () -> S, args: Array<Any?>, priority: Int) : this(StartupPrivilege.User, type, factory, args, priority)

    companion object {
        const val HIGH3 = 1000
        const val HIGH2 = 500
        const val HIGH1 = 200
        const val DEFAULT = 0
        const val LOW1 = -200
        const val LOW2 = -500
        const val LOW3 = -1000

        fun <S : Startup> system(type: StartupType, factory: () -> S, args: Array<Any?>, priority: Int) = StartupDelegate(StartupPrivilege.System, type, factory, args, priority)
    }

    private val startupArgs = StartupArgs(args)
    private lateinit var startup: S

    override fun getValue(thisRef: Any?, property: KProperty<*>): S = startup

    fun create() {
        startup = factory()
    }

    fun init(context: PlatformContext) {
        (startup as SyncStartup).init(context, startupArgs)
    }

    suspend fun initAsync(context: PlatformContext) {
        (startup as AsyncStartup).init(context, startupArgs)
    }
}