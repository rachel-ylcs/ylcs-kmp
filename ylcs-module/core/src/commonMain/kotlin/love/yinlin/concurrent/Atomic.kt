@file:OptIn(ExperimentalAtomicApi::class)
package love.yinlin.concurrent

import love.yinlin.reflect.metaSimpleClassName
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

interface Atomic<T> {
    var value: T
    fun exchange(update: T): T
    fun compareAndSet(expect: T, update: T): Boolean
    fun compareAndExchange(expect: T, update: T): T
    fun incrementAndGet(): T
    fun decrementAndGet(): T
}

fun atomic(value: Boolean) = object : Atomic<Boolean> {
    val delegate = AtomicBoolean(value)
    override var value: Boolean get() = delegate.load()
        set(value) { delegate.store(value) }
    override fun exchange(update: Boolean): Boolean = delegate.exchange(update)
    override fun compareAndSet(expect: Boolean, update: Boolean): Boolean = delegate.compareAndSet(expect, update)
    override fun compareAndExchange(expect: Boolean, update: Boolean): Boolean = delegate.compareAndExchange(expect, update)
    override fun incrementAndGet(): Boolean = throw IllegalStateException("AtomicBoolean can not be increment")
    override fun decrementAndGet(): Boolean = throw IllegalStateException("AtomicBoolean can not be decrement")
}

fun atomic(value: Int) = object : Atomic<Int> {
    val delegate = AtomicInt(value)
    override var value: Int get() = delegate.load()
        set(value) { delegate.store(value) }
    override fun exchange(update: Int): Int = delegate.exchange(update)
    override fun compareAndSet(expect: Int, update: Int): Boolean = delegate.compareAndSet(expect, update)
    override fun compareAndExchange(expect: Int, update: Int): Int = delegate.compareAndExchange(expect, update)
    override fun incrementAndGet(): Int = delegate.addAndFetch(1)
    override fun decrementAndGet(): Int = delegate.addAndFetch(-1)
}

fun atomic(value: Long) = object : Atomic<Long> {
    val delegate = AtomicLong(value)
    override var value: Long get() = delegate.load()
        set(value) { delegate.store(value) }
    override fun exchange(update: Long): Long = delegate.exchange(update)
    override fun compareAndSet(expect: Long, update: Long): Boolean = delegate.compareAndSet(expect, update)
    override fun compareAndExchange(expect: Long, update: Long): Long = delegate.compareAndExchange(expect, update)
    override fun incrementAndGet(): Long = delegate.addAndFetch(1L)
    override fun decrementAndGet(): Long = delegate.addAndFetch(-1L)
}

fun atomic(value: String) = object : Atomic<String> {
    val delegate = AtomicReference(value)
    override var value: String get() = delegate.load()
        set(value) { delegate.store(value) }
    override fun exchange(update: String): String = delegate.exchange(update)
    override fun compareAndSet(expect: String, update: String): Boolean = delegate.compareAndSet(expect, update)
    override fun compareAndExchange(expect: String, update: String): String = delegate.compareAndExchange(expect, update)
    override fun incrementAndGet(): String = throw IllegalStateException("AtomicString can not be increment")
    override fun decrementAndGet(): String = throw IllegalStateException("AtomicString can not be decrement")
}

inline fun <reified T> atomic(value: T) = object : Atomic<T> {
    val delegate = AtomicReference(value)
    override var value: T get() = delegate.load()
        set(value) { delegate.store(value) }
    override fun exchange(update: T): T = delegate.exchange(update)
    override fun compareAndSet(expect: T, update: T): Boolean = delegate.compareAndSet(expect, update)
    override fun compareAndExchange(expect: T, update: T): T = delegate.compareAndExchange(expect, update)
    override fun incrementAndGet(): T = throw IllegalStateException("Atomic ${metaSimpleClassName<T>()} can not be increment")
    override fun decrementAndGet(): T = throw IllegalStateException("Atomic ${metaSimpleClassName<T>()} can not be decrement")
}