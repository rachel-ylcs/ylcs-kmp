package love.yinlin.server

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.Connection
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.JedisPooled

data object Redis {
    private val dataSource = JedisPooled.builder()
        .hostAndPort(Config.Redis.host, Config.Redis.port)
        .clientConfig(DefaultJedisClientConfig.builder()
            .password(Config.Redis.password)
            .timeoutMillis(Config.Redis.timeoutMillis)
            .build()
        ).poolConfig(GenericObjectPoolConfig<Connection>().apply {
            maxTotal = Config.Redis.maxTotal
            maxIdle = Config.Redis.maxIdle
            minIdle = Config.Redis.minIdle
        })
        .build()

    operator fun set(key: String, value: String) { dataSource.set(key, value) }
    fun setex(key: String, value: String, seconds: Long) { dataSource.setex(key, seconds, value) }
    operator fun get(key: String): String? = dataSource.get(key)
    fun remove(key: String) { dataSource.del(key) }
    fun close() = dataSource.close()
}