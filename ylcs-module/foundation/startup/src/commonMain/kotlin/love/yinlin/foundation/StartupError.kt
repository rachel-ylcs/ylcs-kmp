package love.yinlin.foundation

class StartupError(id: String, type: String) : Error("startup $id error in $type")