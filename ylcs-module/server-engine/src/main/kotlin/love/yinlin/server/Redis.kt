package love.yinlin.server

import kotlinx.serialization.Serializable
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.Connection
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.RedisClient

class Redis internal constructor(config: Config) {
    @Serializable
    data class Config(
        val host: String = "localhost",
        val port: Int = 6379,
        val password: String = "",
        val timeoutMillis: Int = 3000,
        val maxTotal: Int = 20,
        val maxIdle: Int = 20,
        val minIdle: Int = 5,
    )

    private val dataSource = RedisClient.builder()
        .hostAndPort(config.host, config.port)
        .clientConfig(DefaultJedisClientConfig.builder()
            .password(config.password)
            .timeoutMillis(config.timeoutMillis)
            .build()
        ).poolConfig(GenericObjectPoolConfig<Connection>().apply {
            maxTotal = config.maxTotal
            maxIdle = config.maxIdle
            minIdle = config.minIdle
        })
        .build()

    operator fun set(key: String, value: String) { dataSource.set(key, value) }
    fun setex(key: String, value: String, seconds: Long) { dataSource.setex(key, seconds, value) }
    operator fun get(key: String): String? = dataSource.get(key)
    fun remove(key: String) { dataSource.del(key) }
    fun close() = dataSource.close()
}