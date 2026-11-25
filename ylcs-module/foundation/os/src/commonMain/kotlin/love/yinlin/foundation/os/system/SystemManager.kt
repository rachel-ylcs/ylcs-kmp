package love.yinlin.foundation.os.system

expect object SystemManager {
    fun getSystemVersion(): String?
    fun getUserName(): String?
}
