package love.yinlin.cs

class RequestTimeoutException(timeout: Long) : Exception("request timeout $timeout ms")

class FailureException(message: String?) : Exception(message)

class CheckerException(name: String, arg: Any?, message: String?) : Exception("$name check error: ${message ?: ""} with $arg")

class UnauthorizedException(message: String?) : Exception(message)