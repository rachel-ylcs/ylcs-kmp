package love.yinlin.api

import kotlin.reflect.KFunction0

abstract class ServerEngine {
    abstract val public: String
    abstract val proxy: Proxy?
    abstract fun scope(): APIScope<out Any, *, *>
    abstract val APIScope<out Any, *, *>.api: List<KFunction0<Unit>>
}