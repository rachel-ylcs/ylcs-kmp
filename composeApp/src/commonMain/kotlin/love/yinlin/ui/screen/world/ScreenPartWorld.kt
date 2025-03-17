package love.yinlin.ui.screen.world

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.Activity
import love.yinlin.extension.*
import love.yinlin.platform.app
import love.yinlin.ui.component.container.Calendar
import love.yinlin.ui.component.container.CalendarState
import love.yinlin.ui.component.image.Banner
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.WebImage

class ScreenPartWorld(model: AppModel) : ScreenPart(model) {
	val flagFirstLoad = launchFlag()
	val activities = mutableStateListOf<Activity>()

	val calendarState = CalendarState()

	fun requestActivity() {
		launch {
			val result = ClientAPI.request(
				route = API.User.Activity.GetActivities
			)
			if (result is Data.Success) activities.replaceAll(result.data)
		}
	}

	fun showActivityDetails(aid: Int) {
		navigate(ScreenActivityDetails(aid))
	}

	fun onDateClick(date: LocalDate) {
		val ts = try {
			DateEx.Formatter.standardDate.format(date)
		} catch (_: Throwable) { null }
		val activity = ts?.let { activities.find { it.ts == ts } }
		if (activity != null) {
			launch {
				showActivityDetails(activity.aid)
			}
		}
	}

	fun onRefreshCalendar() {
		requestActivity()
	}

	@Composable
	private fun BannerLayout(
		spacing: Dp,
		gap: Dp,
		modifier: Modifier = Modifier
	) {
		val activities by rememberDerivedState {
			activities.filter { it.pic != null }
		}
		val pics = remember(activities) {
			activities.map { Picture(it.picPath ?: "") }
		}

		Banner(
			pics = pics,
			spacing = spacing,
			gap = gap,
			interval = 3000L,
			modifier = modifier
		) { pic, index, scale ->
			WebImage(
				uri = pic.image,
				contentScale = ContentScale.Crop,
				modifier = Modifier.fillMaxWidth().aspectRatio(2f)
					.scale(scale).clip(MaterialTheme.shapes.medium),
				onClick = {
					showActivityDetails(activities[index].aid)
				}
			)
		}
	}

	@Composable
	private fun CalendarLayout(
		modifier: Modifier = Modifier
	) {
		val events: Map<LocalDate, String> by rememberDerivedState {
			val events = mutableMapOf<LocalDate, String>()
			for (activity in activities) {
				val ts = activity.ts
				val title = activity.title
				if (ts != null && title != null) {
					try {
						val date = DateEx.Formatter.standardDate.parse(ts)
						events.put(date, title)
					}
					catch (_: Throwable) { }
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

	@Composable
	override fun content() {
		val hasPrivilegeVIPCalendar by rememberDerivedState { app.config.userProfile?.hasPrivilegeVIPCalendar == true }

		Column(modifier = Modifier.fillMaxSize()) {
			Surface(
				modifier = Modifier.fillMaxWidth(),
				shadowElevation = 5.dp
			) {
				Row(
					modifier = Modifier.fillMaxWidth().padding(10.dp),
					horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
				) {
					if (hasPrivilegeVIPCalendar) {
						ClickIcon(
							imageVector = Icons.Outlined.Add,
							onClick = { navigate(ScreenAddActivity) }
						)
					}
					ClickIcon(
						imageVector = Icons.Outlined.Refresh,
						onClick = { onRefreshCalendar() }
					)
				}
			}
			Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
				if (app.isPortrait) {
					Column(
						modifier = Modifier.fillMaxSize(),
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
				else {
					Row(modifier = Modifier.fillMaxSize()) {
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
			}
		}

		LaunchOnce(flagFirstLoad) {
			requestActivity()
		}
	}
}