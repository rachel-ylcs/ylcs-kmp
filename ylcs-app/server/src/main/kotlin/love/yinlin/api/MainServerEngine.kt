package love.yinlin.api

@Suppress("unused")
data object MainServerEngine : ServerEngine() {
    override val public: String = ServerRes.toString()

    override fun APIScope.run() {
        apiCommon(implMap)
        apiUser(implMap)
    }
}