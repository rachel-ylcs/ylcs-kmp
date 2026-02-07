package love.yinlin.compose.ui.node

import androidx.compose.ui.Modifier
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * 当 value == true 时应用 callback
 */
inline fun Modifier.condition(value: Boolean, callback: Modifier.() -> Modifier): Modifier {
    @Suppress("RETURN_VALUE_NOT_USED")
    @OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
    contract {
        callsInPlace(callback, InvocationKind.AT_MOST_ONCE)
        value holdsIn callback
    }
    return if (value) this.callback() else this
}

/**
 * 当 value == true 时应用 ifTrue, 否则应用 ifFalse
 */
inline fun Modifier.condition(value: Boolean, ifTrue: Modifier.() -> Modifier, ifFalse: Modifier.() -> Modifier): Modifier {
    @Suppress("RETURN_VALUE_NOT_USED")
    @OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
    contract {
        callsInPlace(ifTrue, InvocationKind.AT_MOST_ONCE)
        callsInPlace(ifFalse, InvocationKind.AT_MOST_ONCE)
        value holdsIn ifTrue
        !value holdsIn ifFalse
    }
    return if (value) this.ifTrue() else this.ifFalse()
}