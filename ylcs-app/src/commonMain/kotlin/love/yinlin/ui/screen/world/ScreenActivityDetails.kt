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
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.findModify
import love.yinlin.extension.rememberDerivedState
import love.yinlin.platform.app
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.ClickImage
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.common.ScreenImagePreview
import love.yinlin.resources.*

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
		shadowElevation = 5.dp
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
				modifier = Modifier.fillMaxWidth().padding(10.dp),
				verticalArrangement = Arrangement.spacedBy(10.dp),
			) {
				Text(
					text = activity.title ?: "未知活动",
					style = MaterialTheme.typography.displaySmall
				)
				Text(
					text = activity.ts ?: "未知时间",
					style = MaterialTheme.typography.headlineSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

@Stable
class ScreenActivityDetails(model: AppModel, private val args: Args) : Screen<ScreenActivityDetails.Args>(model) {
	@Stable
	@Serializable
	data class Args(val aid: Int) : Screen.Args

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
			verticalArrangement = Arrangement.spacedBy(10.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {
				if (activity.showstart != null) {
					ClickImage(
						res = Res.drawable.img_showstart,
						modifier = Modifier.size(32.dp),
						onClick = {}
					)
				}
				if (activity.damai != null) {
					ClickImage(
						res = Res.drawable.img_damai,
						modifier = Modifier.size(32.dp),
						onClick = {}
					)
				}
				if (activity.maoyan != null) {
					ClickImage(
						res = Res.drawable.img_maoyan,
						modifier = Modifier.size(32.dp),
						onClick = {}
					)
				}
				if (activity.link != null) {
					ClickIcon(
						icon = Icons.Outlined.Link,
						size = 32.dp,
						onClick = {}
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
	override fun Content() {
		val hasPrivilegeVIPCalendar by rememberDerivedState { app.config.userProfile?.hasPrivilegeVIPCalendar == true }

		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = activity?.title ?: "未知活动",
			onBack = { pop() },
			actions = {
				if (hasPrivilegeVIPCalendar) {
					Action(Icons.Outlined.Edit) {
						navigate(ScreenModifyActivity.Args(args.aid))
					}
					ActionSuspend(Icons.Outlined.Delete) {
						deleteActivity()
					}
				}
			},
			slot = slot
		) {
			activity?.let { activity ->
				if (app.isPortrait) {
					Column(
						modifier = Modifier.fillMaxSize().padding(10.dp)
							.verticalScroll(rememberScrollState()),
						verticalArrangement = Arrangement.spacedBy(10.dp)
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
				else {
					Row(modifier = Modifier.fillMaxSize().padding(10.dp)) {
						ActivityDetailsLayout(
							activity = activity,
							modifier = Modifier.weight(2f)
						)
						VerticalDivider(modifier = Modifier.padding(horizontal = 10.dp))
						ActivityPictureLayout(
							activity = activity,
							modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
						)
					}
				}
			}
		}
	}
}