package love.yinlin.cs.service

import eu.vendeli.rethis.ReThis
import love.yinlin.cs.APIScope
import love.yinlin.cs.ServerService
import love.yinlin.extension.catchingNull
import love.yinlin.extension.to
import kotlin.time.Duration.Companion.milliseconds

class RedisService(scope: APIScope) : ServerService(scope) {
    override val name: String = "redis"

    private var client: ReThis? = null

    override suspend fun onStart() {
        val redisConfig: RedisConfig = catchingNull {
            scope.engine.config["redis"]!!.to()
        } ?: RedisConfig()

        client = ReThis(
            host = redisConfig.host,
            port = redisConfig.port,
        ) {
            usePooling = true
            maxConnections = redisConfig.maxConnection
            connectionAcquireTimeout = redisConfig.timeoutMillis.milliseconds

            auth(redisConfig.password.toCharArray(), redisConfig.username)

            pool {
                maxPendingConnections = redisConfig.maxPending
                maxIdleConnections = redisConfig.maxIdle
                minIdleConnections = redisConfig.maxIdle
            }
        }
    }



    override suspend fun onClose() {
        client?.close()
    }
}