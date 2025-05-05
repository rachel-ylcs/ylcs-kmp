package love.yinlin.ui.screen.world

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.ThemeValue
import love.yinlin.common.Uri
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.findModify
import love.yinlin.extension.rememberDerivedState
import love.yinlin.platform.OS
import love.yinlin.platform.Platform
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.common.ScreenImagePreview
import love.yinlin.resources.*
import love.yinlin.ui.component.screen.ActionScope
import love.yinlin.ui.screen.common.ScreenWebpage

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
		shadowElevation = ThemeValue.Shadow.Surface
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
				modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue),
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
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
class ScreenActivityDetails(model: AppModel, private val args: Args) : SubScreen<ScreenActivityDetails.Args>(model) {
	@Stable
	@Serializable
	data class Args(val aid: Int)

	private val activity: Activity? by derivedStateOf {
		worldPart.activities.find { it.aid == args.aid }
	}

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
		if (result is Data.Success) {
			worldPart.activities.findModify(predicate = { it.aid == args.aid }) { this -= it }
			pop()
		}
		else if (result is Data.Error) slot.tip.error(result.message)
	}

	private fun openLink(url: String) {
		OS.ifPlatform(
			Platform.WebWasm, *Platform.Desktop,
			ifTrue = {
				OS.Net.openUrl(url)
			},
			ifFalse = {
				navigate(ScreenWebpage.Args(url))
			}
		)
	}

	@Composable
	private fun ActivityPictureLayout(
		activity: Activity,
		modifier: Modifier = Modifier
	) {
		val pics = remember(activity) {
			activity.pics.map { Picture(activity.picPath(it)) }
		}

		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace)
			) {
				activity.showstart?.let { showstart ->
					ClickIcon(
						res = Res.drawable.img_showstart,
						size = ThemeValue.Size.ExtraIcon,
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
						size = ThemeValue.Size.ExtraIcon,
						onClick = { openLink("https://m.damai.cn/shows/item.html?itemId=${damai}") }
					)
				}
				activity.maoyan?.let { maoyan ->
					ClickIcon(
						res = Res.drawable.img_maoyan,
						size = ThemeValue.Size.ExtraIcon,
						onClick = { openLink("https://show.maoyan.com/qqw#/detail/${maoyan}") }
					)
				}
				activity.link?.let { link ->
					ClickIcon(
						icon = Icons.Outlined.Link,
						size = ThemeValue.Size.ExtraIcon,
						onClick = { openLink(link) }
					)
				}
			}
			Text(
				text = activity.content,
				modifier = Modifier.fillMaxWidth()
			)
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
		Column(
			modifier = Modifier.fillMaxSize().padding(ThemeValue.Padding.EqualValue)
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
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
		Row(modifier = Modifier.fillMaxSize().padding(ThemeValue.Padding.EqualValue)) {
			ActivityDetailsLayout(
				activity = activity,
				modifier = Modifier.weight(2f)
			)
			VerticalDivider(modifier = Modifier.padding(horizontal = ThemeValue.Padding.HorizontalSpace))
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
			Action(Icons.Outlined.Edit) {
				navigate(ScreenModifyActivity.Args(args.aid))
			}
			ActionSuspend(Icons.Outlined.Delete) {
				deleteActivity()
			}
		}
	}

	@Composable
	override fun SubContent(device: Device) = activity?.let {
		when (device.type) {
			Device.Type.PORTRAIT -> Portrait(it)
			Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(it)
		}
	} ?: EmptyBox()
}