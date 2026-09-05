package love.yinlin.cs.service

import kotlinx.serialization.Serializable

@Serializable
data class MysqlConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val name: String = "mysql",
    val username: String = "root",
    val password: String = "",
    val maxPoolSize: Int = 10,
    val idleTimeout: Long = 30000L,
    val maxLifetime: Long = 1800000L,
)