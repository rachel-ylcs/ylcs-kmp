package love.yinlin.extension

import kotlinx.coroutines.sync.Mutex
import love.yinlin.concurrent.atomic
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private class LazyName<T : Any>(private val initializer: (String) -> T) : ReadOnlyProperty<Any?, T> {
    private val binding = atomic<Lazy<T>?>(null)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        binding.value?.let { return it.value }
        val candidate = lazy { initializer(property.name) }
        return (binding.compareAndExchange(null, candidate) ?: candidate).value
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