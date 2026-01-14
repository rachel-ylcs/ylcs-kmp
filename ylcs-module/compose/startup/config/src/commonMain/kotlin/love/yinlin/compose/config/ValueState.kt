package love.yinlin.compose.config

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Stable
abstract class ValueState<T>(
    private val version: String? = null,
    private val stateFactory: (T) -> MutableState<T> = { mutableStateOf(it) }
) : ConfigState, ReadWriteProperty<Any?, T> {
    abstract fun kvGet(key: String): T
    abstract fun kvSet(key: String, value: T)

    private val KProperty<*>.storageKey: String get() = "${this.name}${version}"
    private var state: MutableState<T>? = null

    final override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (state == null) state = stateFactory(kvGet(property.storageKey))
        return state!!.value
    }

    final override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state?.let {
            val oldValue = it.value
            it.value = value
            if (oldValue != value) kvSet(property.storageKey, value)
        } ?: run {
            state = stateFactory(value)
            kvSet(property.storageKey, value)
        }
    }
}