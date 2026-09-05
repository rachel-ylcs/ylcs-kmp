package love.yinlin.cs.service

import eu.vendeli.rethis.ReThis
import eu.vendeli.rethis.command.generic.del
import eu.vendeli.rethis.command.string.get
import eu.vendeli.rethis.command.string.set
import eu.vendeli.rethis.shared.request.string.SetExpire
import eu.vendeli.rethis.types.interfaces.LoggerFactory
import love.yinlin.cs.APIScope
import love.yinlin.cs.ServerService
import love.yinlin.extension.catchingNull
import love.yinlin.extension.to
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RedisService(scope: APIScope) : ServerService(scope) {
    override val name: String = "redis"

    @PublishedApi
    internal var client: ReThis? = null

    override suspend fun onStart() {
        val redisConfig: RedisConfig = catchingNull {
            scope.engine.config[name]!!.to()
        } ?: RedisConfig()

        val logger = scope.logger

        client = ReThis(
            host = redisConfig.host,
            port = redisConfig.port
        ) {
            auth(redisConfig.password.toCharArray(), redisConfig.username)
            loggerFactory = LoggerFactory { logger }
            maxConnections = redisConfig.maxConnection
            connectionAcquireTimeout = redisConfig.timeoutMillis.milliseconds
            usePooling = true
            pool {
                maxPendingConnections = redisConfig.maxPending
                minIdleConnections = redisConfig.minIdle
                maxIdleConnections = redisConfig.maxIdle
            }
        }

        logger.info("Redis Started")
    }

    override suspend fun onClose() {
        client?.close()
    }

    suspend operator fun set(key: String, value: String) {
        client?.set(key, value)
    }

    suspend fun setex(key: String, value: String, time: Duration) {
        client?.set(key, value, SetExpire.Ex(time))
    }

    suspend operator fun get(key: String): String? = client?.get(key)

    suspend fun remove(key: String) {
        client?.del(key)
    }

    suspend inline fun pipeline(crossinline block: suspend RedisService.() -> Unit) {
        client?.pipeline { block() }
    }
}