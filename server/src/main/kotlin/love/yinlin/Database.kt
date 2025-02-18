package love.yinlin

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import love.yinlin.extension.Json
import love.yinlin.extension.json
import love.yinlin.extension.makeArray
import love.yinlin.extension.obj
import java.math.BigDecimal
import java.sql.Connection
import java.sql.Date
import java.sql.SQLIntegrityConstraintViolationException
import java.sql.Statement
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Database {
	private val dataSource = HikariDataSource(HikariConfig().apply {
		jdbcUrl = "jdbc:mysql://${Config.HOST}:${Config.Mysql.PORT}/${Config.Mysql.NAME}"
		username = Config.Mysql.USERNAME
		password = Config.Mysql.PASSWORD
		isAutoCommit = true
		maximumPoolSize = 10
		minimumIdle = 2
		idleTimeout = 30000
		connectionTimeout = 30000
		maxLifetime = 1800000
	})

	val connection: Connection get() = dataSource.connection
}

object SQLConverter {
	private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
	private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
	private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

	@OptIn(ExperimentalStdlibApi::class)
	fun convert(type: String, value: Any?): JsonElement {
		if (value == null) return JsonNull
		return when (type) {
			"BIGINT" -> (value as Long).json
			"BINARY", "BLOB", "LONGBLOB", "MEDIUMBLOB", "TINYBLOB", "VARBINARY" -> (value as ByteArray).toHexString().json
			"BIT" -> (value as Boolean).json
			"CHAR", "TEXT", "VARCHAR", "TINYTEXT", "LONGTEXT" -> (value as String).json
			"DATE" -> try { (value as Date).toLocalDate().format(dateFormatter).json }
			catch (_: Throwable) { JsonNull }
			"DATETIME" -> try { (value as LocalDateTime).format(dateTimeFormatter).json } catch (_: Throwable) { JsonNull }
			"DECIMAL" -> (value as BigDecimal).json
			"DOUBLE" -> (value as Double).json
			"FLOAT" -> (value as Float).json
			"INT", "TINYINT", "MEDIUMINT", "SMALLINT" -> (value as Int).json
			"JSON" -> Json.parseToJsonElement(value as String)
			"TIME" -> try { (value as Time).toLocalTime().format(timeFormatter).json } catch (_: Throwable) { JsonNull }
			"TIMESTAMP" -> try { (value as Timestamp).toLocalDateTime().format(dateTimeFormatter).json } catch (_: Throwable) { JsonNull }
			else -> "$type ${value::class.qualifiedName} $value".json
		}
	}
}

object DB {
	fun throwQuerySQL(sql: String, vararg args: Any?): JsonArray {
		Database.connection.use {
			val statement = it.prepareStatement(sql)
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
							colNames[i] to SQLConverter.convert(colTypes[i], obj)
						}
					}
				}
			}
		}
	}

	fun querySQL(sql: String, vararg args: Any?): JsonArray? = try {
		throwQuerySQL(sql, *args)
	}
	catch (_: Throwable) { null }

	fun throwQuerySQLSingle(sql: String, vararg args: Any?): JsonObject {
		val result = throwQuerySQL(sql, *args)
		if (result.size != 1) throw Throwable("NotSingle ${args.joinToString()}")
		return result[0].obj
	}

	fun querySQLSingle(sql: String, vararg args: Any?): JsonObject? = try {
		val result = throwQuerySQL(sql, *args)
		if (result.size != 1) throw Throwable("NotSingle ${args.joinToString()}")
		result[0].obj
	}
	catch (_: Throwable) { null }

	fun Connection.throwExecuteSQL(sql: String, vararg args: Any?) {
		val statement = this.prepareStatement(sql)
		args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
		if (statement.executeUpdate() <= 0) throw Throwable("NoAffect ${args.joinToString()}")
	}

	fun throwExecuteSQL(sql: String, vararg args: Any?) = Database.connection.use { it.throwExecuteSQL(sql, *args) }

	fun Connection.updateSQL(sql: String, vararg args: Any?): Boolean {
		val statement = this.prepareStatement(sql)
		args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
		return statement.executeUpdate() > 0
	}

	fun updateSQL(sql: String, vararg args: Any?): Boolean = Database.connection.use { it.updateSQL(sql, *args) }

	fun Connection.deleteSQL(sql: String, vararg args: Any?): Boolean = updateSQL(sql, *args)

	fun deleteSQL(sql: String, vararg args: Any?): Boolean = updateSQL(sql, *args)

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

	fun throwInsertSQLDuplicateKey(sql: String, vararg args: Any?): Boolean = Database.connection.use { it.throwInsertSQLDuplicateKey(sql, *args) }

	fun Connection.throwInsertSQLGeneratedKey(sql: String, vararg args: Any?): Long {
		val statement = this.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
		args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
		if (statement.executeUpdate() <= 0) throw Throwable("NoAffect ${args.joinToString()}")
		val keys = statement.generatedKeys
		if (!keys.next()) throw Throwable("NoAffect ${args.joinToString()}")
		return keys.getLong(1)
	}

	fun throwInsertSQLGeneratedKey(sql: String, vararg args: Any?): Long = Database.connection.use { it.throwInsertSQLGeneratedKey(sql, *args) }

	fun throwTransaction(call: (Connection) -> Unit) {
		Database.connection.use {
			it.autoCommit = false
			try {
				call(it)
				it.commit()
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
}