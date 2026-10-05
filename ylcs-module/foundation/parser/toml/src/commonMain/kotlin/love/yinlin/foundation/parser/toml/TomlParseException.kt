package love.yinlin.foundation.parser.toml

class TomlParseException internal constructor(
    message: String,
    val line: Int,
    val column: Int,
    val offset: Int,
) : IllegalArgumentException("$message at line $line, column $column.")