package love.yinlin.ui.screen.msg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastFilter
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.until
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.*
import love.yinlin.data.Data
import love.yinlin.data.ItemKey
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.findSelf
import love.yinlin.extension.rememberDerivedState
import love.yinlin.extension.replaceAll
import love.yinlin.platform.*
import love.yinlin.ui.component.container.Calendar
import love.yinlin.ui.component.container.CalendarState
import love.yinlin.ui.component.image.Banner
import love.yinlin.ui.component.image.IconText
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.layout.SplitLayout
import love.yinlin.ui.component.screen.dialog.FloatingDownloadDialog
import love.yinlin.ui.screen.common.ScreenImagePreview
import love.yinlin.ui.screen.common.ScreenVideo
import love.yinlin.ui.screen.common.ScreenWebpage.Companion.gotoWebPage
import love.yinlin.ui.screen.msg.activity.ScreenActivityDetails
import love.yinlin.ui.screen.msg.activity.ScreenAddActivity
import love.yinlin.ui.screen.msg.douyin.ScreenDouyin
import love.yinlin.ui.screen.msg.pictures.ScreenPictures
import love.yinlin.ui.screen.msg.weibo.*
import kotlin.math.abs

@Stable
class ScreenPartMsg(model: AppModel) : ScreenPart(model) {
	// 当前微博
	var currentWeibo: Weibo? = null
	// 微博处理器
	val processor = object : WeiboProcessor {
		override fun onWeiboClick(weibo: Weibo) {
			currentWeibo = weibo
			navigate<ScreenWeiboDetails>()
		}

		override fun onWeiboAvatarClick(info: WeiboUserInfo) {
			navigate(ScreenWeiboUser.Args(info.id))
		}

		override fun onWeiboLinkClick(arg: String) = gotoWebPage(arg)

		override fun onWeiboTopicClick(arg: String) = gotoWebPage(arg)

		override fun onWeiboAtClick(arg: String) = gotoWebPage(arg)

		override fun onWeiboPicClick(pics: List<Picture>, current: Int) {
			navigate(ScreenImagePreview.Args(pics, current))
		}

		override fun onWeiboPicsDownload(pics: List<Picture>) {
			OS.ifPlatform(
				*Platform.Phone,
				ifTrue = {
					launch {
						slot.loading.openSuspend()
						Coroutines.io {
							for (pic in pics) {
								val url = pic.source
								val filename = url.substringAfterLast('/').substringBefore('?')
								Picker.prepareSavePicture(filename)?.let { (origin, sink) ->
									val result = sink.use {
										val result = app.fileClient.safeDownload(
											url = url,
											sink = it,
											isCancel = { false },
											onGetSize = {},
											onTick = { _, _ -> }
										)
										if (result) Picker.actualSave(filename, origin, sink)
										result
									}
									Picker.cleanSave(origin, result)
								}
							}
						}
						slot.loading.close()
					}
				},
				ifFalse = {
					slot.tip.warning(UnsupportedPlatformText)
				}
			)
		}

		override fun onWeiboVideoClick(pic: Picture) {
			navigate(ScreenVideo.Args(pic.video))
		}

		override fun onWeiboVideoDownload(url: String) {
			val filename = url.substringAfterLast('/').substringBefore('?')
			launch {
				Coroutines.io {
					Picker.prepareSaveVideo(filename)?.let { (origin, sink) ->
						val result = downloadVideoDialog.openSuspend(url, sink) { Picker.actualSave(filename, origin, sink) }
						Picker.cleanSave(origin, result)
					}
				}
			}
		}
	}

	// 活动日历
	val activities = mutableStateListOf<Activity>()
	private val calendarState = CalendarState()

	private suspend fun requestActivity() {
		val result = ClientAPI.request(
			route = API.User.Activity.GetActivities
		)
		if (result is Data.Success) activities.replaceAll(result.data.sorted())
	}

	private fun showActivityDetails(aid: Int) {
		navigate(ScreenActivityDetails.Args(aid))
	}

	private fun onDateClick(date: LocalDate) {
		DateEx.Formatter.standardDate.format(date)
			?.findSelf(activities) { it.ts }
			?.let { showActivityDetails(it.aid) }
	}

	@Composable
	private fun BannerLayout(
		gap: Float,
		shape: Shape,
		modifier: Modifier = Modifier
	) {
		val pics by rememberDerivedState { activities.fastFilter { it.pic != null } }
		BoxWithConstraints(modifier = modifier) {
			Banner(
				pics = pics,
				interval = 5000L,
				gap = gap,
				modifier = Modifier.fillMaxWidth().heightIn(min = maxWidth * (0.5f - gap))
			) { pic, _, scale ->
				Surface(
					modifier = Modifier.fillMaxWidth().aspectRatio(2f).scale(scale),
					shape = shape,
					shadowElevation = ThemeValue.Shadow.Surface
				) {
					WebImage(
						uri = pic.picPath ?: "",
						contentScale = ContentScale.Crop,
						modifier = Modifier.fillMaxSize(),
						onClick = { showActivityDetails(pic.aid) }
					)
				}
			}
		}
	}

	@Composable
	private fun SectionLayout(modifier: Modifier = Modifier) {
		Surface(
			modifier = modifier,
			shape = MaterialTheme.shapes.large,
			shadowElevation = ThemeValue.Shadow.Surface
		) {
			Row(
				modifier = Modifier.fillMaxWidth()
					.padding(ThemeValue.Padding.Value)
					.horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
				verticalAlignment = Alignment.CenterVertically
			) {
				IconText(
					icon = ExtraIcons.Pictures,
					text = "美图",
					onClick = { navigate<ScreenPictures>() }
				)
				IconText(
					icon = ExtraIcons.Weibo,
					text = "微博",
					onClick = { navigate<ScreenWeibo>() }
				)
				IconText(
					icon = ExtraIcons.Chaohua,
					text = "超话",
					onClick = { navigate<ScreenChaohua>() }
				)
				IconText(
					icon = ExtraIcons.Douyin,
					text = "抖音",
					onClick = { navigate<ScreenDouyin>() }
				)
			}
		}
	}

	@Composable
	private fun CalendarLayout(
		modifier: Modifier = Modifier,
		actions: @Composable (ActionScope.() -> Unit)
	) {
		val events: Map<LocalDate, String> by rememberDerivedState {
			val events = mutableMapOf<LocalDate, String>()
			for (activity in activities) {
				val ts = activity.ts
				val title = activity.title
				if (ts != null && title != null) {
					DateEx.Formatter.standardDate.parse(ts)?.let { date ->
						events.put(date, title)
					}
				}
			}
			events
		}

		Surface(
			modifier = modifier,
			shape = MaterialTheme.shapes.extraLarge,
			shadowElevation = ThemeValue.Shadow.Surface
		) {
			Calendar(
				state = calendarState,
				events = events,
				actions = actions,
				modifier = Modifier.fillMaxWidth(),
				onEventClick = { onDateClick(it) }
			)
		}
	}

	@Composable
	private fun CalendarBarItem(
		modifier: Modifier = Modifier,
		activity: Activity,
	) {
		val date = remember(activity) { activity.ts?.let { DateEx.Formatter.standardDate.parse(it) } }
		val interval = remember(date) {
			if (date != null) DateEx.Today.until(date, DateTimeUnit.DAY) else null
		}
		val intervalString = remember(interval) { when {
			interval == null -> ""
			interval == 0 -> "进行中"
			interval > 0 -> ">> ${interval}天"
			interval < 0 -> "<< ${abs(interval)}天"
			else -> ""
		} }
		val intervalColor = when {
			interval == null -> MaterialTheme.colorScheme.onSurface
			interval == 0 -> MaterialTheme.colorScheme.primary
			interval > 0 -> MaterialTheme.colorScheme.secondary
			interval < 0 -> MaterialTheme.colorScheme.tertiary
			else -> MaterialTheme.colorScheme.onSurface
		}

		Surface(
			modifier = modifier,
			shadowElevation = ThemeValue.Shadow.Surface
		) {
			SplitLayout(
				modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualExtraSpace),
				aspectRatio = 0.5f,
				left = {
					Text(
						text = intervalString,
						style = if (LocalDevice.current.type == Device.Type.PORTRAIT) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
						color = intervalColor,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
				},
				right = {
					Text(
						text = remember(activity) { "${activity.title} / ${activity.ts}" },
						style = if (LocalDevice.current.type == Device.Type.PORTRAIT) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
				}
			)
		}
	}

	@Composable
	private fun Portrait() {
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			item(key = ItemKey("Banner")) {
				BannerLayout(
					gap = 0f,
					shape = RectangleShape,
					modifier = Modifier.fillMaxWidth().padding(bottom = ThemeValue.Padding.VerticalSpace)
				)
			}
			item(key = ItemKey("Section")) {
				SectionLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
			}
			item(key = ItemKey("Calendar")) {
				CalendarLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value)) {
					if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
						Action(Icons.Outlined.Add) {
							navigate<ScreenAddActivity>()
						}
					}
					ActionSuspend(Icons.Outlined.Refresh) {
						requestActivity()
					}
				}
			}
			items(
				items = activities,
				key = { it.aid }
			) { activity ->
				CalendarBarItem(
					modifier = Modifier.fillMaxWidth().clickable {
						activity.ts?.let { DateEx.Formatter.standardDate.parse(it) }?.let {
							onDateClick(it)
						}
					},
					activity = activity
				)
			}
		}
	}

	@Composable
	private fun Landscape() {
		Column(modifier = Modifier.fillMaxSize()) {
			Surface(
				modifier = Modifier.fillMaxWidth(),
				shadowElevation = ThemeValue.Shadow.Surface
			) {
				Row(
					modifier = Modifier
						.padding(LocalImmersivePadding.current.withoutBottom)
						.fillMaxWidth()
						.padding(vertical = ThemeValue.Padding.VerticalSpace),
					horizontalArrangement = Arrangement.End,
				) {
					ActionScope.Right.Actions {
						if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
							Action(Icons.Outlined.Add) {
								navigate<ScreenAddActivity>()
							}
						}
						ActionSuspend(Icons.Outlined.Refresh) {
							requestActivity()
						}
					}
				}
			}
			LazyColumn(
				modifier = Modifier.fillMaxWidth().weight(1f),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				item(key = ItemKey("Banner")) {
					BannerLayout(
						gap = 0.3f,
						shape = MaterialTheme.shapes.large,
						modifier = Modifier.fillMaxWidth().padding(vertical = ThemeValue.Padding.VerticalExtraSpace)
					)
				}
				item(key = ItemKey("Section")) {
					SectionLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
				}
				items(
					items = activities,
					key = { it.aid }
				) { activity ->
					CalendarBarItem(
						modifier = Modifier.fillMaxWidth().clickable {
							activity.ts?.let { DateEx.Formatter.standardDate.parse(it) }?.let {
								onDateClick(it)
							}
						},
						activity = activity
					)
				}
			}
		}
	}

	override suspend fun initialize() {
		requestActivity()
	}

	@Composable
	override fun Content() {
		when (LocalDevice.current.type) {
			Device.Type.PORTRAIT -> Portrait()
			Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape()
		}
	}

	private val downloadVideoDialog = FloatingDownloadDialog()

	@Composable
	override fun Floating() {
		downloadVideoDialog.Land()
	}
}