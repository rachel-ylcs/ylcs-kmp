package love.yinlin.extension

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// Reference

data class Reference<T>(var value: T) : ReadWriteProperty<Any?, T> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { this.value = value }
}

// lazyName

fun <T> lazyName(initializer: (String) -> T) = object : ReadOnlyProperty<Any?, T> {
    private lateinit var name: String
    private val instance by lazy { initializer(name) }
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        name = property.name
        return instance
    }
}

// catching

inline fun catching(block: () -> Unit): Unit = try { block() } catch (_: Throwable) {}

inline fun catchingError(clean: () -> Unit = {}, block: () -> Unit): Throwable? = try {
    block()
    null
}
catch (e: Throwable) {
    e
}
finally {
    clean()
}

inline fun <R> catchingNull(block: () -> R): R? = try { block() } catch (_: Throwable) { null }

inline fun <R> catchingDefault(default: R, block: () -> R): R = try { block() } catch (_: Throwable) { default }

inline fun <R> catchingDefault(default: () -> R, block: () -> R): R = try { block() } catch (_: Throwable) { default() }