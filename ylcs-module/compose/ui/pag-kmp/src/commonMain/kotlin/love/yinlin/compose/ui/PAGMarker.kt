package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
expect class PAGMarker(startTime: Long, duration: Long, comment: String) {
    val startTime: Long
    val duration: Long
    val comment: String
}