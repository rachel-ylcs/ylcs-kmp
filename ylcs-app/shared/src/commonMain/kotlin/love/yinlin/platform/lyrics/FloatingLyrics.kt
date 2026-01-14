package love.yinlin.platform.lyrics

import androidx.compose.runtime.*
import love.yinlin.foundation.Context

@Stable
expect class FloatingLyrics() {
    var isAttached: Boolean
        private set

    fun attach()

    fun detach()

    suspend fun initDelay(context: Context)

    fun update()

    @Composable
    fun Content()
}