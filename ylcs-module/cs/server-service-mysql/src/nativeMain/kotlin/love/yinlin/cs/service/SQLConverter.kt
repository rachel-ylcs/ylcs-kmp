package love.yinlin.cs.service

import io.github.smyrgeorge.sqlx4k.ResultSet
import io.github.smyrgeorge.sqlx4k.impl.extensions.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import love.yinlin.extension.DateEx
import love.yinlin.extension.json
import love.yinlin.extension.parseJson
import love.yinlin.extension.toJson

internal object SQLConverter {
    fun convertValue(type: String, value: ResultSet.Row.Column): JsonElement {
        if (value.isNull()) return JsonNull
        return when (type) {
            "BOOLEAN" -> (value.asInt() != 0).json
            "BINARY", "BLOB", "VARBINARY" -> value.asByteArray().json
            "BIT" -> (value.asChar() != Char.MIN_VALUE).json
            "INT", "TINYINT", "SMALLINT", "MEDIUMINT" -> value.asInt().json
            "INT UNSIGNED" -> value.asUInt().toJson()
            "BIGINT" -> value.asLong().json
            "BIGINT UNSIGNED" -> value.asULong().toJson()
            "FLOAT" -> value.asFloat().json
            "DOUBLE", "DECIMAL" -> value.asDouble().json
            "CHAR", "TEXT", "VARCHAR" -> value.asString().json
            "DATE" -> value.asString().json
            "TIME" -> value.asString().json
            "DATETIME", "TIMESTAMP" -> DateEx.Formatter.standardDateTime.format(value.asLocalDateTime()).json
            "JSON" -> value.asString().parseJson
            else -> "[$type -> ${value.asString()}]".json
        }
    }
}