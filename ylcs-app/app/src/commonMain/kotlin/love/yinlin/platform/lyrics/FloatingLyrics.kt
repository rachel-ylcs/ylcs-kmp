package love.yinlin.platform.lyrics

import androidx.compose.runtime.*
import love.yinlin.Context

@Stable
expect class FloatingLyrics(context: Context) {
    var isAttached: Boolean

    suspend fun init()

    @Composable
    fun Content()
}