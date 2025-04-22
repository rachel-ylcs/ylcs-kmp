package love.yinlin.extension

import kotlin.reflect.KProperty

data class Reference<T>(var value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { this.value = value }
}