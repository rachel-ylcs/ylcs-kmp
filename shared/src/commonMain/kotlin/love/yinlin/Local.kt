package love.yinlin

// 本地全局配置
object Local {
	const val NAME: String = "银临茶舍"
	const val VERSION: Int = 300
	const val VERSION_NAME: String = "3.0.0"

	// 客户端配置
	object Client {
		// < ------  开发环境  ------ >
		// 当客户端处于开发环境时, 若协同服务器请求 localhost 否则请求副机
		// 当客户端处于生产环境时, 请求主机
		const val DEVELOPMENT: Boolean = true
		const val WITH_SERVER: Boolean = true

		// < ------  始终浅色模式  ------ >
		// 开启后无视主题设置始终保持浅色
		const val ALWAYS_LIGHT_MODE = false

		// 桌面端配置
		object Desktop {
			// < ------  竖屏模式  ------ >
			// 开启后桌面端将保持竖屏用于调试
			const val ALWAYS_PORTRAIT = false

			// < ------  屏幕比  ------ >
			// 桌面端窗口占屏幕的比例
			const val SCREEN_PERCENT = 0.85f
		}
	}

	// 服务器配置
	object Server {
		// < ------  开发环境  ------ >
		// 当处于开发环境时, 服务器运行在本地, 请求服务时连接副机
		// 当处于生产环境时, 服务器运行在主机, 请求服务时连接主机
		const val DEVELOPMENT: Boolean = true
	}


	// ***********  不可修改处  ***********

	const val MAIN_HOST: String = "yinlin.love"
	private const val SECONDARY_HOST: String = "49.235.151.78"
	private const val LOCAL_HOST: String = "localhost"
	private const val TEST_PORT: Int = 1211

	val ServerHost: String = if (Server.DEVELOPMENT) SECONDARY_HOST else LOCAL_HOST
	val ClientHost: String = if (!Client.DEVELOPMENT) MAIN_HOST else if (Client.WITH_SERVER) LOCAL_HOST else SECONDARY_HOST
	@Suppress("HttpUrlsUsage")
	val ClientUrl: String = if (Client.DEVELOPMENT) "http://$ClientHost:$TEST_PORT" else "https://$ClientHost"
}