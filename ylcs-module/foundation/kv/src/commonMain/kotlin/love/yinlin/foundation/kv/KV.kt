package love.yinlin.foundation.kv

import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

expect class KV(context: PlatformContext, initPath: File) : AutoCloseable {
    override fun close()

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
