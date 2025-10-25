package love.yinlin.service

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class StartupDelegate<S : Startup> internal constructor(
    val privilege: StartupPrivilege,
    val type: StartupType,
    private val factory: () -> S,
    args: Array<Any?>,
    val order: Int,
) : ReadOnlyProperty<Any?, Startup> {
    constructor(type: StartupType, factory: () -> S, args: Array<Any?>, order: Int) : this(StartupPrivilege.User, type, factory, args, order)

    companion object {
        fun <S : Startup> system(type: StartupType, factory: () -> S, args: Array<Any?>, order: Int) = StartupDelegate(StartupPrivilege.System, type, factory, args, order)
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