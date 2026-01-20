package love.yinlin.compose.ui

expect class PAGMarker(startTime: Long, duration: Long, comment: String) {
    val startTime: Long
    val duration: Long
    val comment: String
}