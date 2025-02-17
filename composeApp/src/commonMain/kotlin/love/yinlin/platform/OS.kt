package love.yinlin.platform

import androidx.compose.runtime.Stable
import love.yinlin.ui.component.screen.DialogProgressState

@Stable
expect object OS {
	@Stable
	val platform: Platform // 平台

	fun openURL(url: String)

	suspend fun downloadImage(url: String, state: DialogProgressState)
}