package love.yinlin.compose

import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic

data class LaunchFlag(val value: AtomicBoolean = atomic(false)) {
    inline operator fun invoke(update: () -> Unit = {}, init: () -> Unit) {
        if (value.compareAndSet(expect = false, update = true)) init()
        else update()
    }
}