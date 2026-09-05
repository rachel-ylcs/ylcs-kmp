package love.yinlin.cs.user

class ValidationError(source: String, data: Any? = null) : Throwable() {
    override val message: String = "ValidationError $source $data"
}