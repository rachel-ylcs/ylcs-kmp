package love.yinlin.cs.service

import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.SQLError
import io.github.smyrgeorge.sqlx4k.Statement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.extension.Long
import love.yinlin.extension.Object
import love.yinlin.extension.catchingNull
import love.yinlin.extension.makeArray
import org.intellij.lang.annotations.Language

fun values(count: Int): String = if (count > 0) buildString {
    append(" VALUES(")
    repeat(count - 1) { append("?, ") }
    append("?) ")
}
else " VALUES() "

private fun buildSQLStatement(@Language("SQL") sql: String, vararg args: Any?): Statement {
    val statement = Statement.create(sql)
    args.forEachIndexed { index, any ->
        statement.bind(index, any)
    }
    return statement
}

suspend fun QueryExecutor.throwQuerySQL(@Language("SQL") sql: String, vararg args: Any?): JsonArray {
    val statement = buildSQLStatement(sql, *args)
    val resultSet = fetchAll(statement).getOrThrow()
    val metadata = resultSet.metadata
    val colCount = metadata.getColumnCount()
    val colNames = mutableListOf<String>()
    val colTypes = mutableListOf<String>()

    repeat(colCount) { index ->
        val column = metadata.getColumn(index)
        colNames += column.name
        colTypes += column.type
    }

    return makeArray {
        for (row in resultSet) {
            obj {
                repeat(colCount) { index ->
                    colNames[index] with SQLConverter.convertValue(colTypes[index], row.get(index))
                }
            }
        }
    }
}

suspend fun QueryExecutor.querySQL(@Language("SQL") sql: String, vararg args: Any?): JsonArray? = catchingNull {
    throwQuerySQL(sql, *args)
}

suspend fun QueryExecutor.throwQuerySQLSingle(@Language("SQL") sql: String, vararg args: Any?): JsonObject {
    val result = throwQuerySQL(sql, *args)
    if (result.size != 1) throw IllegalStateException("NotSingle ${args.joinToString()}")
    return result[0].Object
}

suspend fun QueryExecutor.querySQLSingle(@Language("SQL") sql: String, vararg args: Any?): JsonObject? = catchingNull {
    throwQuerySQLSingle(sql, *args)
}

suspend fun QueryExecutor.throwExecuteSQL(@Language("SQL") sql: String, vararg args: Any?) {
    val statement = buildSQLStatement(sql, *args)
    val affectRows = execute(statement).getOrThrow()
    if (affectRows <= 0) throw IllegalStateException("NoAffect ${args.joinToString()}")
}

suspend fun QueryExecutor.updateSQL(@Language("SQL") sql: String, vararg args: Any?): Boolean {
    val statement = buildSQLStatement(sql, *args)
    return execute(statement).getOrDefault(0L) > 0
}

suspend fun QueryExecutor.throwInsertSQLDuplicateKey(@Language("SQL") sql: String, vararg args: Any?): Boolean {
    val statement = buildSQLStatement(sql, *args)
    return try {
        val affectRows = execute(statement).getOrThrow()
        if (affectRows <= 0) throw IllegalStateException("NoAffect ${args.joinToString()}")
        false
    }
    catch (e: Throwable) {
        val msg = (e as? SQLError)?.message ?: throw e // 库没提供获取sql错误码的接口
        val pattern = Regex("\\s(\\d+)\\s*\\(")
        val code = pattern.find(msg)?.groupValues?.get(1)?.toIntOrNull() ?: throw e
        if (code == 1062) true else throw e // 1062: 键重复
    }
}

suspend fun QueryExecutor.throwInsertSQLGeneratedKey(@Language("SQL") sql: String, vararg args: Any?): Long {
    val statement = buildSQLStatement(sql, *args)
    val affectRows = execute(statement).getOrThrow()
    if (affectRows <= 0) throw IllegalStateException("NoAffect ${args.joinToString()}")
    return throwQuerySQLSingle("SELECT LAST_INSERT_ID() AS id")["id"].Long // 库没提供获取生成自增key的接口
}