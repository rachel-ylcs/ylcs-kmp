package love.yinlin.foundation.os.system

expect object SystemManager {
    suspend fun getSystemVersion(): String?
    suspend fun getUserName(): String?
}
