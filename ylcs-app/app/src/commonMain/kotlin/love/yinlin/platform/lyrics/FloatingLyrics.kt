package love.yinlin.platform.lyrics

import androidx.compose.runtime.*
import love.yinlin.Context

@Stable
expect class FloatingLyrics() {
    var isAttached: Boolean
        private set

    fun attach()

    fun detach()

    suspend fun initDelay(context: Context)

    @Composable
    fun Content()
}