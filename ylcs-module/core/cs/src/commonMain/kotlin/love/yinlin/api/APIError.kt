package love.yinlin.api

class RequestTimeoutException(timeout: Long) : Exception("请求超时 $timeout ms")
class FailureException(message: String?) : Exception(message)
class UnauthorizedException(message: String?) : Exception(message)