package love.yinlin.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ui.screen.ScreenMain
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboDetails
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboFollows
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboUser

sealed interface Route {
	@Serializable
	data object Main: Route

	// 微博
	@Serializable
	data object WeiboDetails: Route
	@Serializable
	data class WeiboUser(val id: String): Route
	@Serializable
	data object WeiboFollows: Route

	companion object {
		fun NavGraphBuilder.buildRoute(
			appModel: AppModel
		) {
			composable<Main> {
				ScreenMain(appModel)
			}

			// 微博
			composable<WeiboDetails> {
				ScreenWeiboDetails(appModel)
			}
			composable<WeiboUser> {
				val args = it.toRoute<WeiboUser>()
				ScreenWeiboUser(appModel, args.id)
			}
			composable<WeiboFollows> {
				ScreenWeiboFollows(appModel)
			}
		}
	}
}