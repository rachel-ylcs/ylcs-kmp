package love.yinlin.foundation

expect object LibLoader {
    fun env(name: String)
    fun resource(name: String)
}