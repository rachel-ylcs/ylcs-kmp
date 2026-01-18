package love.yinlin.compose.ui

expect object PAG {
    val sdkVersion: String

    suspend fun init()
}