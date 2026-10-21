package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable

@Stable
data class ImageProgress(val received: Long = 0L, val total: Long? = null) {
    val fraction: Float? get() = total?.takeIf { it > 0L }?.let { (received.toFloat() / it).coerceIn(0f, 1f) }
}
