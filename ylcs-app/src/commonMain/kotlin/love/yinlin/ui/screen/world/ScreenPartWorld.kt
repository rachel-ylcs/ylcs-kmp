package love.yinlin.ui.screen.world

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.until
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalDevice
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.*
import love.yinlin.platform.app
import love.yinlin.ui.component.container.Calendar
import love.yinlin.ui.component.container.CalendarState
import love.yinlin.ui.component.image.Banner
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.SplitLayout
import love.yinlin.ui.component.screen.ActionScope
import kotlin.math.abs

@Stable
class ScreenPartWorld(model: AppModel) : ScreenPart(model) {
	val activities = mutableStateListOf<Activity>()

	private val calendarState = CalendarState()

	private suspend fun requestActivity() {
		val result = ClientAPI.request(
			route = API.User.Activity.GetActivities
		)
		if (result is Data.Success) activities.replaceAll(result.data)
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
	private fun ToolBar(modifier: Modifier = Modifier) {
		val hasPrivilegeVIPCalendar by rememberDerivedState { app.config.userProfile?.hasPrivilegeVIPCalendar == true }

		Surface(
			modifier = modifier,
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
					if (hasPrivilegeVIPCalendar) {
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
	}

    @Composable
	private fun BannerLayout(
		gap: Float,
		shape: Shape,
		modifier: Modifier = Modifier
	) {
		val pics by rememberDerivedState { activities.filter { it.pic != null } }
		Banner(
			pics = pics,
			interval = 5000L,
			gap = gap,
			modifier = modifier
		) { pic, index, scale ->
			Surface(
				modifier = Modifier.fillMaxWidth().aspectRatio(2f).scale(scale),
				shape = shape,
				shadowElevation = ThemeValue.Shadow.Surface
			) {
				WebImage(
					uri = pic.picPath ?: "",
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize(),
					onClick = {
						showActivityDetails(pic.aid)
					}
				)
			}
		}
	}

	@Composable
	private fun CalendarLayout(modifier: Modifier = Modifier) {
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
				modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value),
				left = {
					Text(
						text = intervalString,
						style = MaterialTheme.typography.displayLarge,
						color = intervalColor
					)
				},
				right = {
					Text(
						text = remember(activity) { "${activity.title} / ${activity.ts}" },
						style = MaterialTheme.typography.titleLarge,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
				}
			)
		}
	}

	@Composable
	private fun Portrait(modifier: Modifier = Modifier) {
		Column(
			modifier = modifier,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			BannerLayout(
				gap = 0f,
				shape = RectangleShape,
				modifier = Modifier.fillMaxWidth()
			)
			Box(
				modifier = Modifier.widthIn(max = ThemeValue.Size.PanelWidth).fillMaxWidth().weight(1f),
				contentAlignment = Alignment.Center
			) {
				CalendarLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
			}
		}
	}

	@Composable
	private fun Landscape(modifier: Modifier = Modifier) {
		LazyColumn(
			modifier = modifier,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			item(key = "Banner".itemKey) {
				BannerLayout(
					gap = 0.3f,
					shape = MaterialTheme.shapes.large,
					modifier = Modifier.fillMaxWidth().padding(vertical = ThemeValue.Padding.VerticalExtraSpace)
				)
			}
			items(
				items = activities,
				key = { it.aid }
			) { activity ->
				CalendarBarItem(
					modifier = Modifier.fillMaxWidth()
						.padding(ThemeValue.Padding.Value)
						.clickable {
							activity.ts?.let { DateEx.Formatter.standardDate.parse(it) }?.let {
								onDateClick(it)
							}
						},
					activity = activity
				)
			}
		}
	}

	override suspend fun initialize() {
		requestActivity()
	}

	@Composable
	override fun Content() {
		Column(modifier = Modifier.fillMaxSize()) {
			ToolBar(modifier = Modifier.fillMaxWidth())
			when (LocalDevice.current.type) {
				Device.Type.PORTRAIT -> Portrait(modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.padding(LocalImmersivePadding.current.withoutTop)
				)
				Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.padding(LocalImmersivePadding.current.withoutTop)
				)
			}
		}
	}
}