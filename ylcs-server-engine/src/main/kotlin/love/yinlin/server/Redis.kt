package love.yinlin.server

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.Connection
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.JedisPooled

object Redis {
    private val dataSource = JedisPooled.builder()
        .hostAndPort(Config.Redis.HOST, Config.Redis.PORT)
        .clientConfig(DefaultJedisClientConfig.builder()
            .password(Config.Redis.PASSWORD)
            .timeoutMillis(3000)
            .build()
        ).poolConfig(GenericObjectPoolConfig<Connection>().apply {
            maxTotal = 20
            maxIdle = 20
            minIdle = 5
        })
        .build()

    operator fun set(key: String, value: String) { dataSource.set(key, value) }
    fun setex(key: String, value: String, seconds: Long) { dataSource.setex(key, seconds, value) }
    operator fun get(key: String): String? = dataSource.get(key)
    fun remove(key: String) { dataSource.del(key) }
    fun close() = dataSource.close()
}