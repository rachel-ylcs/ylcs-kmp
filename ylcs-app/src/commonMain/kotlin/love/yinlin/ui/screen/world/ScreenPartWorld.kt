package love.yinlin.ui.screen.world

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.LocalOrientation
import love.yinlin.common.Orientation
import love.yinlin.data.Data
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.DateEx
import love.yinlin.extension.findSelf
import love.yinlin.extension.rememberDerivedState
import love.yinlin.extension.replaceAll
import love.yinlin.platform.app
import love.yinlin.ui.component.container.Calendar
import love.yinlin.ui.component.container.CalendarState
import love.yinlin.ui.component.image.Banner
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.screen.ActionScope

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
	private fun BannerLayout(
		spacing: Dp,
		gap: Dp,
		modifier: Modifier = Modifier
	) {
		val pics by rememberDerivedState { activities.filter { it.pic != null } }

		Banner(
			pics = pics,
			spacing = spacing,
			gap = gap,
			interval = 5000L,
			modifier = modifier
		) { pic, index, scale ->
			Surface(
				modifier = Modifier.fillMaxWidth().aspectRatio(2f).scale(scale),
				shape = MaterialTheme.shapes.large,
				shadowElevation = 5.dp
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
			shadowElevation = 5.dp
		) {
			Calendar(
				state = calendarState,
				events = events,
				modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
				onEventClick = { onDateClick(it) }
			)
		}
	}

	override suspend fun initialize() {
		requestActivity()
	}

	@Composable
	private fun ToolBar(modifier: Modifier = Modifier) {
		val hasPrivilegeVIPCalendar by rememberDerivedState { app.config.userProfile?.hasPrivilegeVIPCalendar == true }

		Surface(
			modifier = modifier,
			shadowElevation = 5.dp
		) {
			Row(
				modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
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
	private fun Portrait(modifier: Modifier = Modifier) {
		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.SpaceBetween
		) {
			BannerLayout(
				spacing = 40.dp,
				gap = 10.dp,
				modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
			)
			CalendarLayout(
				modifier = Modifier.fillMaxWidth().padding(10.dp)
			)
		}
	}

	@Composable
	private fun Landscape(modifier: Modifier = Modifier) {
		Row(modifier = modifier) {
			BannerLayout(
				spacing = 100.dp,
				gap = 50.dp,
				modifier = Modifier.weight(2f).padding(start = 10.dp, top = 10.dp)
			)
			CalendarLayout(
				modifier = Modifier.weight(1f).padding(10.dp)
			)
		}
	}

	@Composable
	override fun Content() {
		Column(modifier = Modifier.fillMaxSize()) {
			ToolBar(modifier = Modifier.fillMaxWidth())
			when (LocalOrientation.current) {
				Orientation.PORTRAIT -> Portrait(modifier = Modifier.fillMaxWidth().weight(1f))
				Orientation.LANDSCAPE -> Landscape(modifier = Modifier.fillMaxWidth().weight(1f))
				Orientation.SQUARE -> {

				}
			}
		}
	}
}