package love.yinlin.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
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

data object Database {
    private val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:mysql://${Config.Mysql.host}:${Config.Mysql.port}/${Config.Mysql.name}"
        username = Config.Mysql.username
        password = Config.Mysql.password
        isAutoCommit = true
        maximumPoolSize = Config.Mysql.maximumPoolSize
        minimumIdle = Config.Mysql.minimumIdle
        idleTimeout = Config.Mysql.idleTimeout
        connectionTimeout = Config.Mysql.connectionTimeout
        maxLifetime = Config.Mysql.maxLifetime
    })

    val connection: Connection get() = dataSource.connection

    fun close() = dataSource.close()
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
    if (statement.executeUpdate() <= 0) throw Throwable("NoAffect ${args.joinToString()}")
    false
}
catch (err: Throwable) {
    if (err is SQLIntegrityConstraintViolationException && err.errorCode == 1062) true
    else throw err
}

fun Connection.throwInsertSQLGeneratedKey(sql: String, vararg args: Any?): Long {
    val statement = this.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
    if (statement.executeUpdate() <= 0) throw Throwable("NoAffect ${args.joinToString()}")
    val keys = statement.generatedKeys
    if (!keys.next()) throw Throwable("NoAffect ${args.joinToString()}")
    return keys.getLong(1)
}

object DB {
    fun throwQuerySQL(sql: String, vararg args: Any?) = Database.connection.use { it.throwQuerySQL(sql, *args) }

    fun querySQL(sql: String, vararg args: Any?) = Database.connection.use { it.querySQL(sql, *args) }

    fun throwQuerySQLSingle(sql: String, vararg args: Any?) = Database.connection.use { it.throwQuerySQLSingle(sql, *args) }

    fun querySQLSingle(sql: String, vararg args: Any?) = Database.connection.use { it.querySQLSingle(sql, *args) }

    fun throwExecuteSQL(sql: String, vararg args: Any?) = Database.connection.use { it.throwExecuteSQL(sql, *args) }

    fun updateSQL(sql: String, vararg args: Any?): Boolean = Database.connection.use { it.updateSQL(sql, *args) }

    fun deleteSQL(sql: String, vararg args: Any?): Boolean = updateSQL(sql, *args)

    fun throwInsertSQLDuplicateKey(sql: String, vararg args: Any?): Boolean = Database.connection.use { it.throwInsertSQLDuplicateKey(sql, *args) }

    fun throwInsertSQLGeneratedKey(sql: String, vararg args: Any?): Long = Database.connection.use { it.throwInsertSQLGeneratedKey(sql, *args) }

    fun <R> throwTransaction(call: (Connection) -> R): R = Database.connection.use {
        it.autoCommit = false
        try {
            val result = call(it)
            it.commit()
            result
        }
        catch (err: Throwable) {
            it.rollback()
            throw err
        }
        finally {
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