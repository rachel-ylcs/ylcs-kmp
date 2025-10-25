package love.yinlin.service

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class StartupDelegate<S : Startup> internal constructor(
    val privilege: StartupPrivilege,
    val type: StartupType,
    private val factory: () -> S,
    private val args: Array<Any?>,
) : ReadOnlyProperty<Any?, Startup> {
    constructor(type: StartupType, factory: () -> S, args: Array<Any?>) : this(StartupPrivilege.User, type, factory, args)

    companion object {
        fun <S : Startup> system(type: StartupType, factory: () -> S, args: Array<Any?>) = StartupDelegate(StartupPrivilege.System, type, factory, args)
    }

    private lateinit var startup: S

    override fun getValue(thisRef: Any?, property: KProperty<*>): S = startup

    fun create() {
        startup = factory()
    }

    fun init(context: PlatformContext) {
        (startup as SyncStartup).init(context, args)
    }

    suspend fun initAsync(context: PlatformContext) {
        (startup as AsyncStartup).init(context, args)
    }
}