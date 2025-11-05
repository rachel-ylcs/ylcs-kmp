package love.yinlin

import androidx.compose.runtime.Stable

@Stable
fun interface StartupLazyFetcher<T> {
    fun fetch(): T
}