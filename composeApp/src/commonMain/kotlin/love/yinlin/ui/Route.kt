package love.yinlin.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.data.common.Picture
import love.yinlin.extension.buildNavTypeMap
import love.yinlin.ui.screen.ScreenMain
import love.yinlin.ui.screen.common.ScreenImagePreview
import love.yinlin.ui.screen.common.ScreenWebPage
import love.yinlin.ui.screen.community.ScreenLogin
import love.yinlin.ui.screen.community.ScreenMail
import love.yinlin.ui.screen.community.ScreenTopic
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboAlbum
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboDetails
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboFollows
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboUser
import love.yinlin.ui.screen.settings.ScreenSettings

sealed interface Route {
	@Serializable
	data object Main: Route

	// 通用
	@Serializable
	data class ImagePreview(val images: List<Picture>, val current: Int) : Route
	@Serializable
	data class WebPage(val url: String): Route

	// 设置
	@Serializable
	data object Settings : Route

	// 微博
	@Serializable
	data object WeiboDetails: Route
	@Serializable
	data class WeiboUser(val id: String): Route
	@Serializable
	data object WeiboFollows: Route
	@Serializable
	data class WeiboAlbum(val album: love.yinlin.data.weibo.WeiboAlbum): Route

	// 社区
	@Serializable
	data object Login : Route
	@Serializable
	data class Topic(val topic: love.yinlin.data.rachel.Topic) : Route
	@Serializable
	data object Mail : Route

	companion object {
		fun NavGraphBuilder.buildRoute(appModel: AppModel) {
			composable<Main> {
				ScreenMain(appModel)
			}

			// 通用
			composable<WebPage> {
				val args = it.toRoute<WebPage>()
				ScreenWebPage(appModel, args.url)
			}

			// 设置
			composable<Settings> {
				ScreenSettings(appModel)
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
			composable<WeiboAlbum>(typeMap = buildNavTypeMap<love.yinlin.data.weibo.WeiboAlbum>()) {
				val args = it.toRoute<WeiboAlbum>()
				ScreenWeiboAlbum(appModel, args.album)
			}
			composable<ImagePreview>(typeMap = buildNavTypeMap<List<Picture>>()) {
				val args = it.toRoute<ImagePreview>()
				ScreenImagePreview(appModel, args.images, args.current)
			}

			// 社区
			composable<Login> {
				ScreenLogin(appModel)
			}
			composable<Topic>(typeMap = buildNavTypeMap<love.yinlin.data.rachel.Topic>()) {
				val args = it.toRoute<Topic>()
				ScreenTopic(appModel, args.topic)
			}
			composable<Mail> {
				ScreenMail(appModel)
			}
		}
	}
}