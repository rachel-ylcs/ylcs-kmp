package love.yinlin.compose

import androidx.compose.runtime.Stable
import love.yinlin.concurrent.Atomic
import love.yinlin.concurrent.atomic

@Stable
data class LaunchFlag(@PublishedApi internal val value: Atomic<Boolean> = atomic(false)) {
    inline operator fun invoke(update: () -> Unit = {}, init: () -> Unit) {
        if (value.compareAndSet(expect = false, update = true)) init()
        else update()
    }
}