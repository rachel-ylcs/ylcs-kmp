package love.yinlin.compose

import androidx.compose.runtime.Stable
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic

@Stable
data class LaunchFlag(@PublishedApi internal val value: AtomicBoolean = atomic(false)) {
    inline operator fun invoke(update: () -> Unit = {}, init: () -> Unit) {
        if (value.compareAndSet(expect = false, update = true)) init()
        else update()
    }
}