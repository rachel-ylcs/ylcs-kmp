package love.yinlin.api

abstract class ServerEngine {
    abstract val public: String
    abstract fun APIScope.run()
}