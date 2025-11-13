package love.yinlin.api

class RequestTimeoutException(timeout: Long) : Exception("连接超时 $timeout ms")
class FailureException(message: String?) : Exception(message)
class UnauthorizedException(message: String?) : Exception(message)