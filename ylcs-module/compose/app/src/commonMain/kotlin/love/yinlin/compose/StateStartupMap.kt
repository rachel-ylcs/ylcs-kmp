package love.yinlin.compose

import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateMapOf
import love.yinlin.foundation.Startup
import love.yinlin.foundation.StartupMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Stable
class StateStartupMap : StartupMap {
    @Stable
    private class StateStartupDelegate(map: Map<String, Startup>, id: String) : ReadOnlyProperty<Any?, Startup?> {
        private val startup by derivedStateOf { map[id] }
        override fun getValue(thisRef: Any?, property: KProperty<*>): Startup? = startup
    }

    private val stateMap: MutableMap<String, Startup> = mutableStateMapOf()
    override fun get(id: String): Startup? = stateMap[id]
    override fun set(id: String, value: Startup) { stateMap[id] = value }
    override fun delegate(id: String): ReadOnlyProperty<Any?, Startup?> = StateStartupDelegate(stateMap, id)
}