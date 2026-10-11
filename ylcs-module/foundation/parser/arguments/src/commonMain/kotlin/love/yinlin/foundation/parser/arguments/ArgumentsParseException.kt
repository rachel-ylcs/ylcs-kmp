package love.yinlin.foundation.parser.arguments

class ArgumentsParseException internal constructor(
    message: String,
    val offset: Int,
    val argumentIndex: Int?,
) : IllegalArgumentException("$message at offset $offset${argumentIndex?.let { " in argument $it" } ?: ""}.")
