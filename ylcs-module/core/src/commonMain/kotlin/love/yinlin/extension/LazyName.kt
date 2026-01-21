package love.yinlin.extension

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