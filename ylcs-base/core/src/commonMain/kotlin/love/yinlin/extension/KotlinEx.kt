package love.yinlin.extension

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// NativeLibrary

@Target(
    AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION,
    AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY
)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class NativeLib(vararg val libs: String)

// Reference

class Reference<T>(var value: T) : ReadWriteProperty<Any?, T> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { this.value = value }
}

class LazyReference<T : Any> : ReadOnlyProperty<Any?, T> {
    lateinit var mValue: T
    fun init(value: T) {
        if (!::mValue.isInitialized) mValue = value
    }
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = mValue
}

// lazyName

inline fun <T : Any> lazyName(crossinline initializer: (String) -> T) = object : ReadOnlyProperty<Any?, T> {
    private lateinit var name: String
    private val instance by lazy { initializer(name) }
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        name = property.name
        return instance
    }
}

// catching


// 无视异常
inline fun catching(block: () -> Unit): Unit = try {
    block()
}
catch (_: Throwable) { }

// 返回异常, clean 清理
inline fun catchingError(block: () -> Unit): Throwable? = try {
    block()
    null
}
catch (e: Throwable) { e }

// onError 返回 null 则重抛异常, clean 清理
inline fun <R : Any> catchingThrow(clean: () -> Unit = {}, onError: (Throwable) -> R? = { null }, block: () -> R): R = try {
    block()
}
catch (e: Throwable) {
    onError(e) ?: throw e
}
finally {
    clean()
}

// 无视异常, 返回 null
inline fun <R> catchingNull(block: () -> R): R? = try {
    block()
}
catch (_: Throwable) {
    null
}

// 无视异常, 返回默认值
inline fun <R> catchingDefault(default: R, block: () -> R): R = try {
    block()
}
catch (_: Throwable) {
    default
}

// 无视异常, 返回默认值
inline fun <R> catchingDefault(default: (Throwable) -> R, block: () -> R): R = try {
    block()
}
catch (e: Throwable) {
    default(e)
}