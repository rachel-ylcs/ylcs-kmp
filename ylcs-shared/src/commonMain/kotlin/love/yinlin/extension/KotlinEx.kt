package love.yinlin.extension

import kotlin.reflect.KProperty

data class Reference<T>(var value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { this.value = value }
}

inline fun catching(block: () -> Unit): Unit = try { block() } catch (_: Throwable) {}
inline fun <R> catchingNull(block: () -> R): R? = try { block() } catch (_: Throwable) { null }
inline fun <R> catchingDefault(default: R, block: () -> R): R = try { block() } catch (_: Throwable) { default }
inline fun <R> catchingDefault(default: () -> R, block: () -> R): R = try { block() } catch (_: Throwable) { default() }