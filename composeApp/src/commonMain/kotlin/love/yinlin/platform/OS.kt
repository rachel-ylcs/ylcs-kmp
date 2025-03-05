package love.yinlin.platform

import love.yinlin.ui.component.screen.DialogProgressState

expect object OS {
	val platform: Platform // 平台

	fun openURL(url: String)

	suspend fun downloadImage(url: String, state: DialogProgressState)
}