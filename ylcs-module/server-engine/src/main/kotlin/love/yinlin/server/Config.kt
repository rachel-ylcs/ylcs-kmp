package love.yinlin.server

import java.util.Properties

data object Config {
    private val prop = Properties().apply {
        val classLoader = Config::class.java.classLoader
        val stream = classLoader.getResourceAsStream("config.properties")!!
        load(stream)
        stream.close()
    }

    data object Mysql {
        val host: String = prop["mysql.host"] as? String ?: "localhost"
        val port: Int = (prop["mysql.port"] as String).toInt()
        val name: String = prop["mysql.name"] as String
        val username: String = prop["mysql.username"] as String
        val password: String = prop["mysql.password"] as String
        val maximumPoolSize: Int = (prop["mysql.maximumPoolSize"] as? String)?.toIntOrNull() ?: 10
        val minimumIdle: Int = (prop["mysql.minimumIdle"] as? String)?.toIntOrNull() ?: 2
        val idleTimeout: Long = (prop["mysql.idleTimeout"] as? String)?.toLongOrNull() ?: 30000L
        val connectionTimeout: Long = (prop["mysql.connectionTimeout"] as? String)?.toLongOrNull() ?: 30000L
        val maxLifetime: Long = (prop["mysql.maxLifetime"] as? String)?.toLongOrNull() ?: 1800000L
    }

    data object Redis {
        val host: String = prop["redis.host"] as? String ?: "localhost"
        val port: Int = (prop["redis.port"] as String).toInt()
        val password: String = prop["redis.password"] as String
        val timeoutMillis: Int = (prop["redis.timeoutMillis"] as? String)?.toIntOrNull() ?: 3000
        val maxTotal: Int = (prop["redis.maxTotal"] as? String)?.toIntOrNull() ?: 20
        val maxIdle: Int = (prop["redis.maxIdle"] as? String)?.toIntOrNull() ?: 20
        val minIdle: Int = (prop["redis.minIdle"] as? String)?.toIntOrNull() ?: 5
    }
}