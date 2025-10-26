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