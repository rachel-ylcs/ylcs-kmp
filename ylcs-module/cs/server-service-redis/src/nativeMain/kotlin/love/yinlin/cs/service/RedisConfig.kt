package love.yinlin.cs.service

import kotlinx.serialization.Serializable

@Serializable
data class RedisConfig(
    val host: String = "localhost",
    val port: Int = 6379,
    val username: String? = null,
    val password: String = "",
    val maxConnection: Int = 5000,
    val timeoutMillis: Int = 10000,
    val maxPending: Int = 1000,
    val maxIdle: Int = 100,
    val minIdle: Int = 10,
)