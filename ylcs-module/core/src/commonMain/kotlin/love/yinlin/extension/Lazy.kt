package love.yinlin.extension

import kotlinx.coroutines.sync.Mutex
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private class LazyName<T : Any>(initializer: (String) -> T) : ReadOnlyProperty<Any?, T> {
    lateinit var name: String
    val instance by lazy { initializer(name) }
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        name = property.name
        return instance
    }
}

/**
 * 与 [lazy] 不同的是在 [lazyName] 中你可以获取到 [lazy] 变量的名字
 * 并且它不会被 `proguard` 等混淆影响
 *
 * ```kotlin
 * val abc by lazyName { name -> assert(name == "abc") }
 * ```
 */
fun <T : Any> lazyName(initializer: (String) -> T): ReadOnlyProperty<Any?, T> = LazyName(initializer)

private class LazyProvider<T : Any>(val initializer: () -> T?) : ReadOnlyProperty<Any?, T?> {
    var cache: T? = null
    val mutex = Mutex()

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        if (cache == null) {
            if (mutex.tryLock()) {
                val newValue = initializer()
                if (newValue != null) cache = newValue
                mutex.unlock()
            }
        }
        return cache
    }
}

/**
 * 与 [lazy] 不同的是 [lazyProvider] 在接受到 null 后不会固化, 直到接受到非空数据后缓存
 */
fun <T : Any> lazyProvider(initializer: () -> T?): ReadOnlyProperty<Any?, T?> = LazyProvider(initializer)