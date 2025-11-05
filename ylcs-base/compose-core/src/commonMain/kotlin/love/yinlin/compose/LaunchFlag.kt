package love.yinlin.compose

import androidx.compose.runtime.Stable
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic

@Stable
data class LaunchFlag(val value: AtomicBoolean = atomic(false)) {
    inline operator fun invoke(update: () -> Unit = {}, init: () -> Unit) {
        if (value.compareAndSet(expect = false, update = true)) init()
        else update()
    }

    suspend operator fun invoke(update: suspend () -> Unit = {}, init: suspend () -> Unit) {
        if (value.compareAndSet(expect = false, update = true)) init()
        else update()
    }
}