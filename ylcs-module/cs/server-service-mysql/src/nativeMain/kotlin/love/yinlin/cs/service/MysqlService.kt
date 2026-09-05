package love.yinlin.cs.service

import io.github.smyrgeorge.sqlx4k.Connection
import io.github.smyrgeorge.sqlx4k.ConnectionPool
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.mysql.MySQL
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.cs.APIScope
import love.yinlin.cs.ServerService
import love.yinlin.extension.catchingNull
import love.yinlin.extension.to
import org.intellij.lang.annotations.Language
import kotlin.time.Duration.Companion.milliseconds

class MysqlService(scope: APIScope) : ServerService(scope) {
    override val name: String = "mysql"

    @PublishedApi
    internal var client: MySQL? = null

    override suspend fun onStart() {
        val mysqlConfig: MysqlConfig = catchingNull {
            scope.engine.config[name]!!.to()
        } ?: MysqlConfig()

        val logger = scope.logger

        client = MySQL(
            url = "mysql://${mysqlConfig.host}:${mysqlConfig.port}/${mysqlConfig.name}",
            username = mysqlConfig.username,
            password = mysqlConfig.password,
            options = ConnectionPool.Options.Builder()
                .maxConnections(mysqlConfig.maxPoolSize)
                .idleTimeout(mysqlConfig.idleTimeout.milliseconds)
                .maxLifetime(mysqlConfig.maxLifetime.milliseconds)
                .build()
        )

        logger.info("Mysql Started")
    }

    override suspend fun onClose() {
        client?.close()
    }

    @PublishedApi
    internal suspend inline fun <R> withConnection(block: (Connection) -> R): R {
        val connection = client!!.acquire().getOrThrow()
        try {
            return block(connection)
        }
        finally {
            connection.close()
        }
    }

    suspend inline fun <R> throwTransaction(crossinline block: suspend (QueryExecutor) -> R): R = client!!.transaction {
        block(this)
    }

    suspend fun throwQuerySQL(@Language("SQL") sql: String, vararg args: Any?): JsonArray = withConnection { connection ->
        connection.throwQuerySQL(sql, *args)
    }

    suspend fun querySQL(@Language("SQL") sql: String, vararg args: Any?): JsonArray? = withConnection { connection ->
        connection.querySQL(sql, *args)
    }

    suspend fun throwQuerySQLSingle(@Language("SQL") sql: String, vararg args: Any?): JsonObject = withConnection { connection ->
        connection.throwQuerySQLSingle(sql, *args)
    }

    suspend fun querySQLSingle(@Language("SQL") sql: String, vararg args: Any?): JsonObject? = withConnection { connection ->
        connection.querySQLSingle(sql, *args)
    }

    suspend fun throwExecuteSQL(@Language("SQL") sql: String, vararg args: Any?) = withConnection { connection ->
        connection.throwExecuteSQL(sql, *args)
    }

    // 更新成功 -> true
    // 更新失败 -> false
    suspend fun updateSQL(@Language("SQL") sql: String, vararg args: Any?): Boolean = withConnection { connection ->
        connection.updateSQL(sql, *args)
    }

    // 删除成功 -> true
    // 删除失败 -> false
    suspend fun deleteSQL(@Language("SQL") sql: String, vararg args: Any?): Boolean = withConnection { connection ->
        connection.deleteSQL(sql, *args)
    }

    // 插入成功 -> false
    // 键重复 -> true
    // 错误 -> throw
    suspend fun throwInsertSQLDuplicateKey(@Language("SQL") sql: String, vararg args: Any?): Boolean = withConnection { connection ->
        connection.throwInsertSQLDuplicateKey(sql, *args)
    }

    suspend fun throwInsertSQLGeneratedKey(@Language("SQL") sql: String, vararg args: Any?): Long = withConnection { connection ->
        connection.throwInsertSQLGeneratedKey(sql, *args)
    }
}