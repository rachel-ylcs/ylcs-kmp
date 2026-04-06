package love.yinlin.foundation

import kotlin.properties.ReadOnlyProperty

interface StartupMap {
    operator fun get(id: String): Startup?
    operator fun set(id: String, value: Startup)
    fun delegate(id: String): ReadOnlyProperty<Any?, Startup?>
}