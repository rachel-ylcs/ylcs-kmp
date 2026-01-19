package love.yinlin.extension

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Reference<T>(var value: T) : ReadWriteProperty<Any?, T> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { this.value = value }
}

interface BaseLazyReference<T : Any> : ReadOnlyProperty<Any?, T> {
    val isInit: Boolean
    fun init(value: T)
}

class LazyReference<T : Any> : BaseLazyReference<T> {
    private lateinit var mValue: T
    override val isInit: Boolean get() = ::mValue.isInitialized
    override fun init(value: T) {
        if (!::mValue.isInitialized) mValue = value
    }
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = mValue
}