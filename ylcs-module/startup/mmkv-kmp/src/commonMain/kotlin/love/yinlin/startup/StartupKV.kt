package love.yinlin.startup

import kotlinx.io.files.Path
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import love.yinlin.extension.catching
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import love.yinlin.foundation.Context
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.StartupFetcher
import love.yinlin.foundation.SyncStartup

@StartupFetcher(index = 0, name = "initPath", returnType = Path::class)
expect class StartupKV() : SyncStartup {
    override fun init(context: Context, args: StartupArgs)

    fun set(key: String, value: Boolean, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: Int, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: Long, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: Float, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: Double, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: String, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: ByteArray, expire: Int = KVExpire.NEVER)
    inline fun <reified T : Any> get(key: String, default: T): T
    operator fun contains(key: String): Boolean
    fun remove(key: String)
}

inline fun <reified T> StartupKV.setJson(key: String, value: T, expire: Int = KVExpire.NEVER) = catching { set(key, value.toJsonString(), expire) }

fun <T> StartupKV.setJson(serializer: SerializationStrategy<T>, key: String, value: T, expire: Int = KVExpire.NEVER) = catching { set(key, value.toJsonString(serializer), expire) }

inline fun <reified T> StartupKV.getJson(key: String, defaultFactory: () -> T): T = catchingDefault({ defaultFactory() }) {
    val json = get(key, "")
    require(json.isNotEmpty())
    json.parseJsonValue()
}

inline fun <T> StartupKV.getJson(deserializer: DeserializationStrategy<T>, key: String, defaultFactory: () -> T): T = catchingDefault({ defaultFactory() }) {
    val json = get(key, "")
    require(json.isNotEmpty())
    json.parseJsonValue(deserializer)
}