package love.yinlin.startup

import androidx.compose.runtime.*
import kotlinx.serialization.serializer
import love.yinlin.compose.config.CacheState
import love.yinlin.compose.config.ListState
import love.yinlin.compose.config.MapState
import love.yinlin.compose.config.Patches
import love.yinlin.compose.config.ValueState
import love.yinlin.coroutines.cpuContext
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import love.yinlin.extension.lazyName
import love.yinlin.foundation.Startup
import love.yinlin.foundation.StartupFactory
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import kotlin.coroutines.CoroutineContext

@Stable
open class StartupConfig(
    pool: StartupPool,
    private val version: Int,
    private val patches: Patches
) : Startup(pool) {
    companion object {
        inline fun <reified S : StartupConfig> custom(
            version: Int,
            patches: Patches,
            crossinline factory: (StartupPool, Int, Patches) -> S
        ): StartupFactory<S> = object : StartupFactory<S> {
            override val id: String = StartupID<S>()
            override val dependencies: List<String> = listOf(StartupID<StartupKV>())
            override val dispatcher: CoroutineContext = cpuContext
            override fun build(pool: StartupPool): S = factory(pool, version, patches)
        }
    }

    @PublishedApi
    internal val kv: StartupKV = pool.requireClass()

    override suspend fun init() {
        for (patch in patches) {
            val key = "#patch#${patch.name}"
            if (!kv.get(key, false) && (patch.version == null || patch.version >= version) && patch.enabled) {
                if (patch.attach()) kv.set(key, true)
            }
        }
    }

    inline fun <reified T : Enum<T>> enumState(
        default: T,
        version: String? = null
    ) = object : ValueState<T>(version) {
        override fun kvGet(key: String): T = kv.get<String>(key, default.toJsonString()).parseJsonValue<T?>() ?: default
        override fun kvSet(key: String, value: T) = kv.set(key, value.toJsonString())
    }

    fun booleanState(
        default: Boolean,
        version: String? = null
    ) = object : ValueState<Boolean>(version) {
        override fun kvGet(key: String): Boolean = kv.get(key, default)
        override fun kvSet(key: String, value: Boolean) { kv.set(key, value) }
    }

    fun intState(
        default: Int,
        version: String? = null
    ) = object : ValueState<Int>(version, { mutableIntStateOf(it) }) {
        override fun kvGet(key: String) = kv.get(key, default)
        override fun kvSet(key: String, value: Int) { kv.set(key, value) }
    }

    fun longState(
        default: Long,
        version: String? = null
    ) = object : ValueState<Long>(version, { mutableLongStateOf(it) }) {
        override fun kvGet(key: String) = kv.get(key, default)
        override fun kvSet(key: String, value: Long) { kv.set(key, value) }
    }

    fun floatState(
        default: Float,
        version: String? = null
    ) = object : ValueState<Float>(version, { mutableFloatStateOf(it) }) {
        override fun kvGet(key: String) = kv.get(key, default)
        override fun kvSet(key: String, value: Float) { kv.set(key, value) }
    }

    fun doubleState(
        default: Double,
        version: String? = null
    ) = object : ValueState<Double>(version, { mutableDoubleStateOf(it) }) {
        override fun kvGet(key: String) = kv.get(key, default)
        override fun kvSet(key: String, value: Double) { kv.set(key, value) }
    }

    fun stringState(
        default: String,
        version: String? = null
    ) = object : ValueState<String>(version) {
        override fun kvGet(key: String) = kv.get(key, default)
        override fun kvSet(key: String, value: String) { kv.set(key, value) }
    }

    inline fun <reified T> jsonState(
        version: String? = null,
        crossinline defaultFactory: () -> T
    ) = object : ValueState<T>(version) {
        override fun kvGet(key: String): T = kv.getJson(key, defaultFactory)
        override fun kvSet(key: String, value: T) { kv.setJson(key, value) }
    }

    fun cacheState() = object : CacheState() {
        override fun kvGet(key: String): Long = kv.get(key, 0L)
        override fun kvSet(key: String, value: Long) { kv.set(key, value) }
    }

    inline fun <reified T> listState(
        version: String? = null,
        noinline defaultFactory: () -> List<T> = { emptyList() }
    ) = lazyName { name ->
        ListState(
            kv = kv,
            name = name,
            version = version,
            itemSerializer = serializer<T>(),
            defaultFactory = defaultFactory
        )
    }

    inline fun <reified K, reified V> mapState(
        version: String? = null,
        noinline defaultFactory: () -> Map<K, V> = { emptyMap() }
    ) = lazyName { name ->
        MapState(
            kv = kv,
            name = name,
            version = version,
            keySerializer = serializer<K>(),
            valueSerializer = serializer<V>(),
            defaultFactory = defaultFactory
        )
    }
}