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
import love.yinlin.common.ThemeColor
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.Activity
import love.yinlin.extension.rememberDerivedState
import love.yinlin.platform.app
import love.yinlin.ui.Screen
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.ClickImage
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.common.ScreenImagePreview
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.img_damai
import ylcs_kmp.composeapp.generated.resources.img_maoyan
import ylcs_kmp.composeapp.generated.resources.img_showstart

@Composable
private fun ActivityInfoLayout(
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
					style = MaterialTheme.typography.displayMedium
				)
				Text(
					text = activity.ts ?: "未知时间",
					style = MaterialTheme.typography.headlineMedium,
					color = ThemeColor.fade
				)
			}
		}
	}
}

@Stable
@Serializable
data class ScreenActivityDetails(val aid: Int) : Screen<ScreenActivityDetails.Model> {
	inner class Model(model: AppModel, activity: Activity?) : Screen.Model(model) {
		var activity: Activity? by mutableStateOf(activity)

		fun onPicClick(pics: List<Picture>, index: Int) {
			navigate(ScreenImagePreview(pics, index))
		}

		@Composable
		fun ActivityPictureLayout(
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
							imageVector = Icons.Outlined.Link,
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
	}

	override fun model(model: AppModel): Model = Model(
		model = model,
		activity = model.part<ScreenPartWorld>().activities.find { it.aid == aid }
	)

	@Composable
	override fun content(model: Model) {
		val hasPrivilegeVIPCalendar by rememberDerivedState { app.config.userProfile?.hasPrivilegeVIPCalendar == true }

		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = model.activity?.title ?: "未知活动",
			onBack = { model.pop() },
			actions = {
				if (hasPrivilegeVIPCalendar) {
					ClickIcon(
						imageVector = Icons.Outlined.Edit,
						modifier = Modifier.padding(end = 10.dp),
						onClick = {}
					)
					ClickIcon(
						imageVector = Icons.Outlined.Delete,
						modifier = Modifier.padding(end = 10.dp),
						onClick = {}
					)
				}
			}
		) {
			model.activity?.let { activity ->
				if (app.isPortrait) {
					Column(
						modifier = Modifier.fillMaxSize().padding(10.dp)
							.verticalScroll(rememberScrollState()),
						verticalArrangement = Arrangement.spacedBy(10.dp)
					) {
						ActivityInfoLayout(
							activity = activity,
							modifier = Modifier.fillMaxWidth()
						)
						model.ActivityPictureLayout(
							activity = activity,
							modifier = Modifier.fillMaxWidth()
						)
					}
				}
				else {
					Row(modifier = Modifier.fillMaxSize().padding(10.dp)) {
						ActivityInfoLayout(
							activity = activity,
							modifier = Modifier.weight(2f)
						)
						VerticalDivider(modifier = Modifier.padding(horizontal = 10.dp))
						model.ActivityPictureLayout(
							activity = activity,
							modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
						)
					}
				}
			}
		}
	}
}