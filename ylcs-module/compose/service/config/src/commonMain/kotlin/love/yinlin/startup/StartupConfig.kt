package love.yinlin.startup

import androidx.compose.runtime.*
import kotlinx.serialization.serializer
import love.yinlin.config.CacheState
import love.yinlin.config.ListState
import love.yinlin.config.MapState
import love.yinlin.config.ValueState
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import love.yinlin.platform.getJson
import love.yinlin.platform.setJson
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.StartupFetcher
import love.yinlin.SyncStartup
import love.yinlin.extension.lazyName

@StartupFetcher(index = 0, name = "kv", returnType = StartupKV::class)
@Stable
open class StartupConfig : SyncStartup() {
    lateinit var kv: StartupKV

    override fun init(context: Context, args: StartupArgs) {
        kv = args.fetch(0)
    }

    inline fun <reified T : Enum<T>> enumState(
        default: T,
        version: String? = null
    ) = object : ValueState<T>(version) {
        override fun kvGet(key: String): T = kv.get<String>(key, default.toJsonString()).parseJsonValue() ?: default
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