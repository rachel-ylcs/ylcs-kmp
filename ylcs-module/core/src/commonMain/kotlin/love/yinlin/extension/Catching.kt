@file:OptIn(ExperimentalContracts::class)
package love.yinlin.extension

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * 无视异常
 */
inline fun catching(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    try { block() }
    catch (_: Throwable) { }
}

/**
 * 返回异常
 */
inline fun catchingError(block: () -> Unit): Throwable? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        block()
        null
    }
    catch (e: Throwable) { e }
}

/**
 * 无视异常, 返回 null
 */
inline fun <R> catchingNull(block: () -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try { block() }
    catch (_: Throwable) { null }
}

/**
 * 无视异常, 返回默认值
 */
inline fun <R> catchingDefault(default: R, block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try { block() }
    catch (_: Throwable) { default }
}

/**
 * 无视异常, 返回默认值
 */
inline fun <R> catchingDefault(default: (Throwable) -> R, block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try { block() }
    catch (e: Throwable) { default(e) }
}