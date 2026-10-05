package love.yinlin.foundation.parser.yaml

class YamlParseException internal constructor(
    message: String,
    val line: Int,
    val column: Int,
    val offset: Int,
) : IllegalArgumentException("$message at line $line, column $column.")