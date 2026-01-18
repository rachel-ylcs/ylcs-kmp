package love.yinlin.compose.ui

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T, R> T.internalCloseable(block: (T) -> R, clean: T.() -> Unit): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        exception.let { cause ->
            if (cause == null) clean()
            else {
                try {
                    clean()
                } catch (closeException: Throwable) {
                    cause.addSuppressed(closeException)
                }
            }
        }
    }
}

inline fun <R> PAGImage.use(block: (PAGImage) -> R): R = internalCloseable(block) { close() }