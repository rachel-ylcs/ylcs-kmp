package love.yinlin.api

abstract class APIPath<Request : Any, Response : Any, Files: Any, Method : APIMethod> protected constructor(
    private val path: String
) {
    override fun toString(): String = path
}