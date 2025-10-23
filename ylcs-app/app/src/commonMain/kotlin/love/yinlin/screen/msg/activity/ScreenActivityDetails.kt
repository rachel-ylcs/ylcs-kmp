package love.yinlin.screen.msg.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.Serializable
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.uri.Uri
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.findModify
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.img_damai
import love.yinlin.resources.img_maoyan
import love.yinlin.resources.img_showstart
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.screen.common.ScreenImagePreview
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.common.ScreenWebpage
import love.yinlin.screen.msg.SubScreenMsg

@Composable
private fun ActivityDetailsLayout(
	activity: Activity,
	modifier: Modifier = Modifier
) {
	val picPath = remember(activity) {
		activity.picPath ?: activity.pics.firstOrNull()?.let { activity.picPath(it) }
	}

	Surface(
		modifier = modifier,
		shadowElevation = CustomTheme.shadow.surface
	) {
		Column(modifier = Modifier.fillMaxWidth()) {
			if (picPath == null) {
				Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) {
					EmptyBox()
				}
			}
			else {
				WebImage(
					uri = picPath,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxWidth().aspectRatio(2f)
				)
			}
			Column(
				modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue),
				verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
			) {
				Text(
					text = activity.title ?: "未知活动",
					style = MaterialTheme.typography.titleLarge
				)
				Text(
					text = activity.ts ?: "未知时间",
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

@Stable
class ScreenActivityDetails(manager: ScreenManager, private val args: Args) : Screen<ScreenActivityDetails.Args>(manager) {
	@Stable
	@Serializable
	data class Args(val aid: Int)

	private val activities = manager.get<ScreenMain>().get<SubScreenMsg>().activities

	private val activity: Activity? by derivedStateOf { activities.find { it.aid == args.aid } }

	private fun onPicClick(pics: List<Picture>, index: Int) {
		navigate(ScreenImagePreview.Args(pics, index))
	}

	private suspend fun deleteActivity() {
		val result = ClientAPI.request(
			route = API.User.Activity.DeleteActivity,
			data = API.User.Activity.DeleteActivity.Request(
				token = app.config.userToken,
				aid = args.aid
			)
		)
        when (result) {
            is Data.Success -> {
				activities.findModify(predicate = { it.aid == args.aid }) { this -= it }
                pop()
            }
            is Data.Failure -> slot.tip.error(result.message)
        }
	}

	@Composable
	private fun ActivityPictureLayout(
		activity: Activity,
		modifier: Modifier = Modifier
	) {
		val pics = remember(activity) {
			activity.pics.fastMap { Picture(activity.picPath(it)) }
		}

		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace)
			) {
				activity.showstart?.let { showstart ->
					ClickIcon(
						res = Res.drawable.img_showstart,
                        tip = "打开秀动",
						size = CustomTheme.size.mediumIcon,
						onClick = {
							launch {
								val uri = Uri.parse(showstart)
								if (uri == null) slot.tip.warning("链接已失效")
								else if (!OS.Application.startAppIntent(uri)) slot.tip.warning("未安装秀动")
							}
						}
					)
				}
				activity.damai?.let { damai ->
					ClickIcon(
						res = Res.drawable.img_damai,
                        tip = "打开大麦",
						size = CustomTheme.size.mediumIcon,
						onClick = { ScreenWebpage.gotoWebPage("https://m.damai.cn/shows/item.html?itemId=${damai}") { navigate(it) } }
					)
				}
				activity.maoyan?.let { maoyan ->
					ClickIcon(
						res = Res.drawable.img_maoyan,
                        tip = "打开猫眼",
						size = CustomTheme.size.mediumIcon,
						onClick = { ScreenWebpage.gotoWebPage("https://show.maoyan.com/qqw#/detail/${maoyan}") { navigate(it) } }
					)
				}
				activity.link?.let { link ->
					ClickIcon(
						icon = Icons.Outlined.Link,
                        tip = "打开链接",
						size = CustomTheme.size.mediumIcon,
						onClick = { ScreenWebpage.gotoWebPage(link) { navigate(it) } }
					)
				}
			}
			SelectionContainer {
				Text(
					text = activity.content,
					modifier = Modifier.fillMaxWidth()
				)
			}
			NineGrid(
				pics = pics,
				modifier = Modifier.fillMaxWidth(),
				onImageClick = { onPicClick(pics, it) },
				onVideoClick = {}
			)
		}
	}

	@Composable
	private fun Portrait(activity: Activity) {
		Column(modifier = Modifier
			.padding(LocalImmersivePadding.current)
			.fillMaxSize()
			.padding(CustomTheme.padding.equalValue)
			.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
		) {
			ActivityDetailsLayout(
				activity = activity,
				modifier = Modifier.fillMaxWidth()
			)
			ActivityPictureLayout(
				activity = activity,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}

	@Composable
	private fun Landscape(activity: Activity) {
		Row(modifier = Modifier
			.padding(LocalImmersivePadding.current)
			.fillMaxSize()
			.padding(CustomTheme.padding.equalValue)
		) {
			ActivityDetailsLayout(
				activity = activity,
				modifier = Modifier.weight(2f)
			)
			VerticalDivider(modifier = Modifier.padding(horizontal = CustomTheme.padding.horizontalSpace))
			ActivityPictureLayout(
				activity = activity,
				modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
			)
		}
	}

	override val title: String by derivedStateOf { activity?.title ?: "未知活动" }

	@Composable
	override fun ActionScope.RightActions() {
		val hasPrivilegeVIPCalendar by rememberDerivedState { app.config.userProfile?.hasPrivilegeVIPCalendar == true }
		if (hasPrivilegeVIPCalendar) {
			Action(Icons.Outlined.Edit, "编辑") {
				navigate(ScreenModifyActivity.Args(args.aid))
			}
			ActionSuspend(Icons.Outlined.Delete, "删除") {
				if (slot.confirm.openSuspend(content = "删除活动")) {
					deleteActivity()
				}
			}
		}
	}

	@Composable
	override fun Content(device: Device) = activity?.let {
        when (device.type) {
            Device.Type.PORTRAIT -> Portrait(it)
            Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(it)
        }
	} ?: EmptyBox()
}