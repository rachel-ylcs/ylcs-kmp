package love.yinlin

// 本地全局配置
object Local {
	const val APP_NAME: String = "ylcs"
	const val NAME: String = "银临茶舍"
	const val VERSION: Int = 301
	const val VERSION_NAME: String = "3.0.1"

	// 客户端配置
	object Client {
		const val DEVELOPMENT: Boolean = false
	}

	// 服务器配置
	object Server {

	}


	// ***********  不可修改处  ***********

	const val MAIN_HOST: String = "yinlin.love"
	private const val LOCAL_HOST: String = "localhost"
	private const val TEST_PORT: Int = 1211

	val ServerHost: String = LOCAL_HOST
	val ClientHost: String = if (!Client.DEVELOPMENT) MAIN_HOST else LOCAL_HOST
	val ClientUrl: String = if (Client.DEVELOPMENT) "http://$ClientHost:$TEST_PORT" else "https://api.$ClientHost"
}