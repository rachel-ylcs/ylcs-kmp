package love.yinlin.compose.ui.input

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf

@Stable
class StepperState(initialValue: Int = 0, val min: Int = Int.MIN_VALUE, val max: Int = Int.MAX_VALUE) {
    init {
        require(min <= max) { "Stepper min must not exceed max" }
    }

    private val currentValue = mutableIntStateOf(initialValue.coerceIn(min, max))

    var value: Int
        get() = currentValue.intValue
        set(newValue) {
            currentValue.intValue = newValue.coerceIn(min, max)
        }

    internal fun step(increase: Boolean): Boolean {
        val current = currentValue.intValue
        val limit = if (increase) max else min
        if (current == limit) return false
        currentValue.intValue = if (increase) current + 1 else current - 1
        return true
    }
}