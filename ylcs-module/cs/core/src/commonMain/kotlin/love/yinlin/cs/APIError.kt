package love.yinlin.cs

class RequestTimeoutException(timeout: Long) : Exception("request timeout $timeout ms")
class FailureException(message: String?) : Exception(message)
class UnauthorizedException(message: String?) : Exception(message)