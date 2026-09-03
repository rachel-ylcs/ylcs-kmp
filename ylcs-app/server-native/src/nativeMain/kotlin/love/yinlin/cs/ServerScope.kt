package love.yinlin.cs

class ServerScope(engine: ServerEngine) : APIScope(engine) {
    override fun api() {
        commonAPI()
    }
}