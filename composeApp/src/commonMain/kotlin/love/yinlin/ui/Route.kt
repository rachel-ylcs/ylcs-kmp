package love.yinlin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.Topic
import love.yinlin.extension.buildNavTypeMap
import love.yinlin.ui.screen.ScreenMain
import love.yinlin.ui.screen.common.ScreenImagePreview
import love.yinlin.ui.screen.common.ScreenWebpage
import love.yinlin.ui.screen.community.ScreenLogin
import love.yinlin.ui.screen.community.ScreenMail
import love.yinlin.ui.screen.community.ScreenTopic
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboAlbum
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboDetails
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboFollows
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboUser
import love.yinlin.ui.screen.settings.ScreenSettings
import love.yinlin.ui.screen.world.ScreenActivityDetails
import love.yinlin.ui.screen.world.ScreenAddActivity
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KType

@Stable
interface Screen<M : Screen.Model> {
	open class Model(private val model: AppModel) : ViewModel() {
		fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
		fun navigate(route: Screen<*>, options: NavOptions? = null, extras: Navigator.Extras? = null) = model.navigate(route, options, extras)
		fun pop() = model.pop()

		@Suppress("PropertyName")
		val _model: AppModel get() = model
		inline fun <reified P : ScreenPart> part(): P = _model.part()
	}

	fun model(model: AppModel): M

	@Composable
	fun content(model: M)
}

private data class ScreenRouteScope(
	val builder: NavGraphBuilder,
	val model: AppModel
)

private inline fun <reified S : Screen<M>, reified M : Screen.Model> ScreenRouteScope.screen(
	typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap()
) {
	builder.composable<S>(typeMap = typeMap) {
		val args = remember(it) { it.toRoute<S>() }
		val screenModel = viewModel { args.model(model) }
		args.content(screenModel)
	}
}

fun NavGraphBuilder.buildRoute(appModel: AppModel) = with(ScreenRouteScope(this, appModel)) {
	// 主页
	screen<ScreenMain, ScreenMain.Model>()
	// 通用
	screen<ScreenWebpage, ScreenWebpage.Model>()
	screen<ScreenImagePreview, ScreenImagePreview.Model>(buildNavTypeMap<List<Picture>>())
	// 设置
	screen<ScreenSettings, ScreenSettings.Model>()
	// 微博
	screen<ScreenWeiboDetails, ScreenWeiboDetails.Model>()
	screen<ScreenWeiboUser, ScreenWeiboUser.Model>()
	screen<ScreenWeiboFollows, ScreenWeiboFollows.Model>()
	screen<ScreenWeiboAlbum, ScreenWeiboAlbum.Model>()
	// 社区
	screen<ScreenLogin, ScreenLogin.Model>()
	screen<ScreenTopic, ScreenTopic.Model>(buildNavTypeMap<Topic>())
	screen<ScreenMail, ScreenMail.Model>()
	// 世界
	screen<ScreenActivityDetails, ScreenActivityDetails.Model>()
	screen<ScreenAddActivity, ScreenAddActivity.Model>()
}