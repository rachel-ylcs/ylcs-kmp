package love.yinlin.compose.data

import androidx.compose.runtime.Stable

@Stable
expect class ItemKey(value: String) {
    internal val value: String
}