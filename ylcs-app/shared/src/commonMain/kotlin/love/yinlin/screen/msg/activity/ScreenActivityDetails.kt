package love.yinlin.screen.msg.activity

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import love.yinlin.app
import love.yinlin.compose.collection.toStableList
import love.yinlin.common.ExtraIcons
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.data.Picture
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.uri.Uri
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.findModify
import love.yinlin.shared.resources.*
import love.yinlin.compose.ui.image.MiniImage
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.SimpleEmptyBox
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.cs.*
import love.yinlin.platform.Platform
import love.yinlin.screen.common.ScreenImagePreview
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.common.ScreenWebpage
import love.yinlin.screen.community.BoxText
import love.yinlin.screen.msg.SubScreenMsg
import love.yinlin.uri.UriGenerator
import org.jetbrains.compose.resources.DrawableResource

@Composable
private fun OutlinedClickIcon(
	text: String,
	icon: Any,
	onClick: () -> Unit
) {
	Box(modifier = Modifier
		.clip(MaterialTheme.shapes.large)
		.border(CustomTheme.border.small, MaterialTheme.colorScheme.onBackground, MaterialTheme.shapes.large)
	) {
		Row(
			modifier = Modifier.clickable(onClick = onClick).padding(CustomTheme.padding.value),
			horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
			verticalAlignment = Alignment.CenterVertically
		) {
			when (icon) {
				is DrawableResource -> MiniImage(res = icon, modifier = Modifier.size(CustomTheme.size.mediumIcon))
				is ImageVector -> MiniImage(icon = icon, modifier = Modifier.size(CustomTheme.size.mediumIcon))
			}
			Text(
				text = text,
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onBackground,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}

@Stable
class ScreenActivityDetails(manager: ScreenManager, private val aid: Int) : Screen(manager) {
	private val activities = manager.get<ScreenMain>().get<SubScreenMsg>().activities

	private val activity: Activity? by derivedStateOf { activities.find { it.aid == aid } }

	private suspend fun deleteActivity() {
		ApiActivityDeleteActivity.request(app.config.userToken, aid) {
			activities.findModify(predicate = { it.aid == aid }) { this -= it }
			pop()
		}.errorTip
	}

	@Composable
	private fun ActivityInfoLayout(
		activity: Activity,
		modifier: Modifier = Modifier
	) {
		val coverPath = remember(activity) { activity.photo.coverPath?.url ?: activity.photo.posters.firstOrNull()?.let { activity.photo.posterPath(it) }?.url }

		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
		) {
			if (coverPath != null) {
				WebImage(
					uri = coverPath,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxWidth().aspectRatio(2f)
				)
			}

			Text(
				text = remember(activity) { activity.title ?: activity.shortTitle ?: "未知活动" },
				style = MaterialTheme.typography.titleLarge,
				modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value)
			)
			NormalText(
				text = remember(activity) { activity.ts?.let { "时间: $it" } ?: "未知时间" },
				icon = Icons.Outlined.Timer
			)
			activity.tsInfo?.let { tsInfo ->
				Text(
					text = tsInfo,
					modifier = Modifier.padding(horizontal = CustomTheme.padding.horizontalExtraSpace * 3)
				)
			}
			NormalText(
				text = remember(activity) { activity.location?.let { "地点: $it" } ?: "未知地点" },
				icon = Icons.Outlined.AddLocation
			)

			Text(
				text = "票价",
				style = MaterialTheme.typography.labelMedium,
				modifier = Modifier.padding(horizontal = CustomTheme.padding.horizontalSpace)
			)
			Row(
				modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.equalSpace).horizontalScroll(rememberScrollState()),
				verticalAlignment = Alignment.CenterVertically
			) {
				for (item in activity.price) {
					Surface(
						modifier = Modifier.padding(CustomTheme.padding.equalValue)
							.border(CustomTheme.border.small, MaterialTheme.colorScheme.onBackground, MaterialTheme.shapes.large),
						shape = MaterialTheme.shapes.large,
						shadowElevation = CustomTheme.shadow.surface
					) {
						Column(
							modifier = Modifier.clickable {}.padding(CustomTheme.padding.equalExtraValue),
							horizontalAlignment = Alignment.CenterHorizontally,
							verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
						) {
							Text(
								text = "￥${item.value}",
								style = MaterialTheme.typography.displaySmall,
								color = MaterialTheme.colorScheme.primary
							)
							Text(
								text = item.name,
								color = MaterialTheme.colorScheme.onSurface
							)
						}
					}
				}
			}

			val link = activity.link
			val showLink = remember(activity) { link.enabled }
			if (showLink) {
				Text(
					text = "演出链接",
					style = MaterialTheme.typography.labelMedium,
					modifier = Modifier.padding(CustomTheme.padding.value)
				)
				FlowRow(
					modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.equalSpace),
					horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
					verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
				) {
					link.showstart?.ifEmpty { null }?.let { showstart ->
						OutlinedClickIcon(
							text = "秀动",
							icon = Res.drawable.img_showstart,
							onClick = {
								launch {
									val uri = Uri.parse(showstart)
									if (uri == null) slot.tip.warning("链接已失效")
									else if (!app.os.application.startAppIntent(uri)) slot.tip.warning("未安装秀动")
								}
							}
						)
					}
					link.damai?.ifEmpty { null }?.let { damai ->
						OutlinedClickIcon(
							text = "大麦",
							icon = Res.drawable.img_damai,
							onClick = { ScreenWebpage.gotoWebPage("https://m.damai.cn/shows/item.html?itemId=${damai}") { navigate(::ScreenWebpage, it) } }
						)
					}
					link.maoyan?.ifEmpty { null }?.let { maoyan ->
						OutlinedClickIcon(
							text = "猫眼",
							icon = Res.drawable.img_maoyan,
							onClick = { ScreenWebpage.gotoWebPage("https://show.maoyan.com/qqw#/detail/${maoyan}") { navigate(::ScreenWebpage, it) } }
						)
					}
					link.link?.ifEmpty { null }?.let { link ->
						OutlinedClickIcon(
							text = "直播",
							icon = Icons.Outlined.Link,
							onClick = { ScreenWebpage.gotoWebPage(link) { navigate(::ScreenWebpage, it) } }
						)
					}

					val qqGroupUri = remember(activity) {
						Platform.use(*Platform.Phone,
							ifTrue = {
								link.qqGroupPhone?.ifEmpty { null }?.let { group -> UriGenerator.qqGroup(group) }
							},
							ifFalse = {
								link.qqGroupLink?.ifEmpty { null }?.let { q -> UriGenerator.qqGroupLink(q) }
							}
						)
					}
					qqGroupUri?.let { uri ->
						OutlinedClickIcon(
							text = "官群",
							icon = ExtraIcons.QQ,
							onClick = {
								launch {
									if (!app.os.application.startAppIntent(uri)) slot.tip.warning("未安装QQ")
								}
							}
						)
					}
				}
			}

			Text(
				text = "开售时间",
				style = MaterialTheme.typography.labelMedium,
				modifier = Modifier.padding(CustomTheme.padding.value)
			)
			if (activity.saleTime.isEmpty()) {
				Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) { SimpleEmptyBox() }
			}
			else {
				Column(
					modifier = Modifier.fillMaxWidth(),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
				) {
					activity.saleTime.fastForEachIndexed { index, item ->
						Surface(
							modifier = Modifier.padding(horizontal = CustomTheme.padding.horizontalSpace).fillMaxWidth()
								.border(CustomTheme.border.small, MaterialTheme.colorScheme.onBackground, MaterialTheme.shapes.large),
							shape = MaterialTheme.shapes.large,
							color = if (index == 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
							shadowElevation = CustomTheme.shadow.surface
						) {
							Box(modifier = Modifier.clickable {}.padding(CustomTheme.padding.extraValue)) {
								Text(
									text = "${index + 1}.  $item",
									style = MaterialTheme.typography.labelMedium,
									color = if (index == 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
								)
							}
						}
					}
				}
			}

			activity.content?.ifEmpty { null }?.let { content ->
				Text(
					text = "服务说明",
					style = MaterialTheme.typography.labelMedium,
					modifier = Modifier.padding(CustomTheme.padding.value)
				)
                SelectionBox {
					Text(
						text = content,
						modifier = Modifier.padding(CustomTheme.padding.value)
					)
				}
			}

			Space()
		}
	}

	@Composable
	private fun ActivityPhotoLayout(
		activity: Activity,
		modifier: Modifier = Modifier
	) {
		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
		) {
			Text(
				text = "演出阵容",
				style = MaterialTheme.typography.labelMedium,
				modifier = Modifier.padding(CustomTheme.padding.value)
			)
			if (activity.lineup.isEmpty()) {
				Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) {
					SimpleEmptyBox()
				}
			}
			else {
				FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalSpace)) {
					for (item in activity.lineup) {
						BoxText(
							text = item,
							color = MaterialTheme.colorScheme.primary
						)
					}
				}
			}

			val seatPath = remember(activity) { activity.photo.seatPath?.url }
			Text(
				text = "座位图",
				style = MaterialTheme.typography.labelMedium,
				modifier = Modifier.padding(CustomTheme.padding.value)
			)
			Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) {
				if (seatPath != null) {
					WebImage(
						uri = seatPath,
						contentScale = ContentScale.Crop,
						modifier = Modifier.fillMaxSize()
					)
				}
				else SimpleEmptyBox()
			}

			val pics = remember(activity) {
				activity.photo.posters.fastMap { Picture(activity.photo.posterPath(it).url) }.toStableList()
			}
			Text(
				text = "海报",
				style = MaterialTheme.typography.labelMedium,
				modifier = Modifier.padding(CustomTheme.padding.value)
			)
			if (pics.isEmpty()) {
				Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) {
					SimpleEmptyBox()
				}
			}
			else {
				NineGrid(
					pics = pics,
					modifier = Modifier.fillMaxWidth(),
					onImageClick = {
						navigate(::ScreenImagePreview, pics, it)
					}
				) { modifier, pic, contentScale, onClick ->
					WebImage(
						uri = pic.image,
						contentScale = contentScale,
						modifier = modifier,
						onClick = onClick
					)
				}
			}
			Space()
		}
	}

	@Composable
	private fun ActivityPlaylistLayout(
		activity: Activity,
		modifier: Modifier = Modifier
	) {
		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
		) {
			Text(
				text = "歌单",
				style = MaterialTheme.typography.labelMedium,
				modifier = Modifier.padding(CustomTheme.padding.value)
			)
			if (activity.playlist.isEmpty()) {
				Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) {
					SimpleEmptyBox()
				}
			}
			else {
				Column(
					modifier = Modifier.fillMaxWidth(),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
				) {
					for (item in activity.playlist) {
						Surface(
							modifier = Modifier.padding(horizontal = CustomTheme.padding.horizontalSpace).fillMaxWidth()
								.border(CustomTheme.border.small, MaterialTheme.colorScheme.onBackground, MaterialTheme.shapes.large),
							shape = MaterialTheme.shapes.large,
							color = MaterialTheme.colorScheme.primaryContainer,
							shadowElevation = CustomTheme.shadow.surface
						) {
							Box(modifier = Modifier.clickable {}.padding(CustomTheme.padding.extraValue)) {
								Text(
									text = item,
									style = MaterialTheme.typography.labelMedium,
									color = MaterialTheme.colorScheme.onPrimaryContainer,
								)
							}
						}
					}
				}
			}
			Space()
		}
	}

	@Composable
	private fun Portrait(activity: Activity) {
		Column(
			modifier = Modifier
				.padding(LocalImmersivePadding.current)
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
		) {
			ActivityInfoLayout(activity = activity, modifier = Modifier.fillMaxWidth())
			ActivityPhotoLayout(activity = activity, modifier = Modifier.fillMaxWidth())
			ActivityPlaylistLayout(activity = activity, modifier = Modifier.fillMaxWidth())
		}
	}

	@Composable
	private fun Landscape(activity: Activity) {
		Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
			ActivityInfoLayout(activity = activity, modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
			VerticalDivider()
			ActivityPhotoLayout(activity = activity, modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
			VerticalDivider()
			ActivityPlaylistLayout(activity = activity, modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
		}
	}

	override val title: String by derivedStateOf { activity?.shortTitle ?: activity?.title ?: "未知活动" }

	@Composable
	override fun ActionScope.RightActions() {
		val hasPrivilegeVIPCalendar by rememberDerivedState { app.config.userProfile?.hasPrivilegeVIPCalendar == true }
		if (hasPrivilegeVIPCalendar) {
			Action(Icons.Outlined.Edit, "编辑") {
				navigate(::ScreenModifyActivity, aid)
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