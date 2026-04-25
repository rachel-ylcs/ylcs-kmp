package love.yinlin.foundation

class StartupError(id: String, type: String, cause: Throwable?) : Throwable("startup $id error in $type.\ncause: ${cause?.stackTraceToString()}")