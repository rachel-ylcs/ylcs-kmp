package love.yinlin.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import love.yinlin.extension.Object
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingNull
import love.yinlin.extension.json
import love.yinlin.extension.makeArray
import love.yinlin.extension.parseJson
import java.math.BigDecimal
import java.sql.Connection
import java.sql.Date
import java.sql.SQLIntegrityConstraintViolationException
import java.sql.Statement
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Database internal constructor(config: Config) {
    @Serializable
    data class Config(
        val host: String = "localhost",
        val port: Int = 3306,
        val name: String = "mysql",
        val username: String = "root",
        val password: String = "",
        val maximumPoolSize: Int = 10,
        val minimumIdle: Int = 2,
        val idleTimeout: Long = 30000L,
        val connectionTimeout: Long = 30000L,
        val maxLifetime: Long = 1800000L,
    )

    private val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:mysql://${config.host}:${config.port}/${config.name}"
        username = config.username
        password = config.password
        isAutoCommit = true
        maximumPoolSize = config.maximumPoolSize
        minimumIdle = config.minimumIdle
        idleTimeout = config.idleTimeout
        connectionTimeout = config.connectionTimeout
        maxLifetime = config.maxLifetime
    })

    private val connection: Connection get() = dataSource.connection

    fun close() = dataSource.close()

    fun throwQuerySQL(sql: String, vararg args: Any?) = connection.use { it.throwQuerySQL(sql, *args) }

    fun querySQL(sql: String, vararg args: Any?) = connection.use { it.querySQL(sql, *args) }

    fun throwQuerySQLSingle(sql: String, vararg args: Any?) = connection.use { it.throwQuerySQLSingle(sql, *args) }

    fun querySQLSingle(sql: String, vararg args: Any?) = connection.use { it.querySQLSingle(sql, *args) }

    fun throwExecuteSQL(sql: String, vararg args: Any?) = connection.use { it.throwExecuteSQL(sql, *args) }

    fun updateSQL(sql: String, vararg args: Any?): Boolean = connection.use { it.updateSQL(sql, *args) }

    fun deleteSQL(sql: String, vararg args: Any?): Boolean = updateSQL(sql, *args)

    fun throwInsertSQLDuplicateKey(sql: String, vararg args: Any?): Boolean = connection.use { it.throwInsertSQLDuplicateKey(sql, *args) }

    fun throwInsertSQLGeneratedKey(sql: String, vararg args: Any?): Long = connection.use { it.throwInsertSQLGeneratedKey(sql, *args) }

    fun <R : Any> throwTransaction(call: (Connection) -> R): R = connection.use {
        it.autoCommit = false
        try {
            val result = call(it)
            it.commit()
            result
        } catch (e: Throwable) {
            it.rollback()
            throw e
        } finally {
            it.autoCommit = true
        }
    }
}

fun values(count: Int): String = if (count > 0) buildString {
    append(" VALUES(")
    repeat(count - 1) { append("?, ") }
    append("?) ")
}
else " VALUES() "

fun Connection.throwQuerySQL(sql: String, vararg args: Any?): JsonArray {
    val statement = prepareStatement(sql)
    args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
    val resultSet = statement.executeQuery()
    val metadata = resultSet.metaData
    val col = metadata.columnCount
    val colNames = mutableListOf("")
    val colTypes = mutableListOf("")
    for (i in 1..col) {
        colNames += metadata.getColumnLabel(i)
        colTypes += metadata.getColumnTypeName(i)
    }
    return makeArray {
        while (resultSet.next()) {
            obj {
                for (i in 1..col) {
                    val obj = resultSet.getObject(i)
                    colNames[i] with SQLConverter.convert(colTypes[i], obj)
                }
            }
        }
    }
}

fun Connection.querySQL(sql: String, vararg args: Any?): JsonArray? = catchingNull { throwQuerySQL(sql, *args) }

fun Connection.throwQuerySQLSingle(sql: String, vararg args: Any?): JsonObject {
    val result = throwQuerySQL(sql, *args)
    if (result.size != 1) throw Throwable("NotSingle ${args.joinToString()}")
    return result[0].Object
}

fun Connection.querySQLSingle(sql: String, vararg args: Any?): JsonObject? = catchingNull {
    val result = throwQuerySQL(sql, *args)
    if (result.size != 1) throw Throwable("NotSingle ${args.joinToString()}")
    result[0].Object
}

fun Connection.throwExecuteSQL(sql: String, vararg args: Any?) {
    val statement = this.prepareStatement(sql)
    args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
    if (statement.executeUpdate() <= 0) throw Throwable("NoAffect ${args.joinToString()}")
}

fun Connection.updateSQL(sql: String, vararg args: Any?): Boolean {
    val statement = this.prepareStatement(sql)
    args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
    return statement.executeUpdate() > 0
}

fun Connection.deleteSQL(sql: String, vararg args: Any?): Boolean = updateSQL(sql, *args)

fun Connection.throwInsertSQLDuplicateKey(sql: String, vararg args: Any?): Boolean = try {
    val statement = this.prepareStatement(sql)
    args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
    if (statement.executeUpdate() <= 0) throw IllegalStateException("NoAffect ${args.joinToString()}")
    false
} catch(e: Throwable) {
    if (e is SQLIntegrityConstraintViolationException && e.errorCode == 1062) true
    else throw e
}

fun Connection.throwInsertSQLGeneratedKey(sql: String, vararg args: Any?): Long {
    val statement = this.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
    if (statement.executeUpdate() <= 0) throw Throwable("NoAffect ${args.joinToString()}")
    val keys = statement.generatedKeys
    if (!keys.next()) throw Throwable("NoAffect ${args.joinToString()}")
    return keys.getLong(1)
}

data object SQLConverter {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun convert(type: String, value: Any?): JsonElement {
        if (value == null) return JsonNull
        return when (type) {
            "BIGINT" -> (value as Long).json
            "BINARY", "BLOB", "LONGBLOB", "MEDIUMBLOB", "TINYBLOB", "VARBINARY" -> (value as ByteArray).json
            "BIT" -> (value as Boolean).json
            "CHAR", "TEXT", "VARCHAR", "TINYTEXT", "LONGTEXT" -> (value as String).json
            "DATE" -> catchingDefault(JsonNull) { (value as Date).toLocalDate().format(dateFormatter).json }
            "DATETIME" -> catchingDefault(JsonNull) { (value as LocalDateTime).format(dateTimeFormatter).json }
            "DECIMAL" -> (value as BigDecimal).json
            "DOUBLE" -> (value as Double).json
            "FLOAT" -> (value as Float).json
            "INT", "TINYINT", "MEDIUMINT", "SMALLINT" -> (value as Int).json
            "JSON" -> (value as String).parseJson
            "TIME" -> catchingDefault(JsonNull) { (value as Time).toLocalTime().format(timeFormatter).json }
            "TIMESTAMP" -> catchingDefault(JsonNull) { (value as Timestamp).toLocalDateTime().format(dateTimeFormatter).json }
            else -> "$type ${value::class.qualifiedName} $value".json
        }
    }

    fun convertTime(ts: String): Long = catchingDefault(0L) { LocalDateTime.parse(ts, dateTimeFormatter).toInstant(ZoneOffset.ofHours(8)).toEpochMilli() }
}