package love.yinlin.cs

abstract class ServerService(protected val scope: APIScope) {
    abstract val name: String
    open suspend fun onStart() { }
    open suspend fun onClose() { }
}