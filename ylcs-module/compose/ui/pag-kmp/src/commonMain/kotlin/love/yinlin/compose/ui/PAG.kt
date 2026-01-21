package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
expect object PAG {
    val sdkVersion: String

    suspend fun init()
}