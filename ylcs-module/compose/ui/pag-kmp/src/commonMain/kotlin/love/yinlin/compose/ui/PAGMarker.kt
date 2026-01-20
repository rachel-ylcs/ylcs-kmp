package love.yinlin.compose.ui

expect class PAGMarker {
    constructor(startTime: Long, duration: Long, comment: String)

    val startTime: Long
    val duration: Long
    val comment: String
}