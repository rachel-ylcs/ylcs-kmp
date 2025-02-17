package love.yinlin

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.time.Duration

object Redis {
	class RedisConnection(val jedis: Jedis) : AutoCloseable {
		override fun close() { jedis.close() }
	}

	val dataSource = JedisPool(JedisPoolConfig().apply {
		maxTotal = 20
		maxIdle = 20
		minIdle = 5
		setMaxWait(Duration.ofMillis(3000))
	}, Config.HOST, Config.Redis.PORT, 3000, Config.Redis.PASSWORD)

	inline fun <R> use(block: (Jedis) -> R): R = RedisConnection(dataSource.resource).use { block(it.jedis) }
}