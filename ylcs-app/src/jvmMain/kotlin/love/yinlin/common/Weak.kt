package love.yinlin.common

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

class Weak<T : Any>(initializer: () -> T?) {
    var weakRef = WeakReference<T?>(initializer())

    constructor(): this({ null })

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return weakRef.get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        weakRef = WeakReference(value)
    }
}
