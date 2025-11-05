package love.yinlin.config

import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableLongStateOf
import love.yinlin.extension.DateEx
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Stable
abstract class CacheState : ConfigState, ReadWriteProperty<Any?, Long> {
    companion object {
        const val UPDATE = Long.MAX_VALUE
    }

    abstract fun kvGet(key: String): Long
    abstract fun kvSet(key: String, value: Long)

    private var state: MutableLongState? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
        if (state == null) state = mutableLongStateOf(kvGet(property.name))
        return state!!.value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
        val newCacheValue = DateEx.CurrentLong
        if (state == null) state = mutableLongStateOf(newCacheValue)
        else state!!.value = newCacheValue
        kvSet(property.name, newCacheValue)
    }
}