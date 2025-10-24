package love.yinlin.service

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class StartupDelegate<S : Startup> internal constructor(
    val privilege: StartupPrivilege,
    val type: StartupType,
    private val factory: () -> S
) : ReadOnlyProperty<Service, Startup> {
    constructor(type: StartupType, factory: () -> S) : this(StartupPrivilege.System, type, factory)

    private lateinit var startup: S

    override fun getValue(thisRef: Service, property: KProperty<*>): S = startup

    fun create() {
        startup = factory()
    }

    fun init() {
        (startup as SyncStartup).init()
    }

    suspend fun initAsync() {
        (startup as AsyncStartup).init()
    }
}