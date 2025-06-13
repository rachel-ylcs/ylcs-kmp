package love.yinlin

// 本地全局配置
object Local {
	const val APP_NAME: String = "ylcs"
	const val NAME: String = "银临茶舍"
	const val VERSION: Int = 311
	const val VERSION_NAME: String = "3.1.1"

	// 客户端配置
	object Client {
		const val DEVELOPMENT: Boolean = false
	}

	// ***********  不可修改处  ***********

	const val MAIN_HOST: String = "yinlin.love"
	const val LOCAL_HOST: String = "localhost"

	@Suppress("HttpUrlsUsage")
	val ClientUrl: String = run {
		val clientHost = if (Client.DEVELOPMENT) LOCAL_HOST else MAIN_HOST
		if (Client.DEVELOPMENT) "http://$clientHost:1211" else "https://api.$clientHost"
	}
}