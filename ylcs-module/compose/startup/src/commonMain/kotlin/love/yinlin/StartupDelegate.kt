package love.yinlin

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Stable
class StartupDelegate<S : Startup>(
    val priority: Int,
    private val factory: () -> S,
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
    private var serviceName: String? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): S {
        serviceName = property.name.ifEmpty { null }
        return startup
    }

    val privilege: StartupPrivilege get() = startup.privilege
    val isSync: Boolean get() = startup is SyncStartup
    val isAsync: Boolean get() = startup is AsyncStartup

    override fun toString(): String = when {
        !::startup.isInitialized -> "uninitialized"
        startup::class.qualifiedName != null -> startup.toString()
        serviceName != null -> startup.toString().replace("null", serviceName!!)
        else -> startup.toString().replace("null", "unnamed")
    }

    fun createStartup() {
        startup = factory()
    }

    fun initStartup(context: Context, later: Boolean) {
        if (later) startup.initLater(context, startupArgs)
        else startup.init(context, startupArgs)
    }

    suspend fun CoroutineScope.initStartup(context: Context, later: Boolean) {
        with(startup) {
            if (later) this@initStartup.initLater(context, startupArgs)
            else this@initStartup.init(context, startupArgs)
        }
    }

    fun destroyStartup(context: Context, before: Boolean) {
        if (before) startup.destroy(context, startupArgs)
        else startup.destroyBefore(context, startupArgs)
    }
}