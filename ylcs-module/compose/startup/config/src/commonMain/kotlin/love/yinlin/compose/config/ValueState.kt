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
        val currentState = state
        return if (currentState != null) currentState.value
        else {
            val newState = stateFactory(kvGet(property.storageKey))
            state = newState
            newState.value
        }
    }

    final override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val currentState = state
        if (currentState != null) {
            val oldValue = currentState.value
            currentState.value = value
            if (oldValue != value) kvSet(property.storageKey, value)
        }
        else {
            state = stateFactory(value)
            kvSet(property.storageKey, value)
        }
    }
}