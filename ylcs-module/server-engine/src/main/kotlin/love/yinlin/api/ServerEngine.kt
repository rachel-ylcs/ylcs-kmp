package love.yinlin.api

import kotlin.reflect.KFunction0

abstract class ServerEngine {
    abstract val public: String
    abstract val APIScope.api: List<KFunction0<Unit>>
    open val proxy: Proxy? = null
}