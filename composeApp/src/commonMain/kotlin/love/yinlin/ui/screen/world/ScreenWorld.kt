package love.yinlin.ui.screen.world

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.ktor.utils.io.*
import kotlinx.coroutines.ensureActive
import kotlinx.datetime.LocalDate
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
import love.yinlin.ui.screen.MainModel

class WorldModel(val mainModel: MainModel) {
	val flagFirstLoad = launchFlag()
	val activities = mutableStateListOf<Activity>()
	val calendarState = CalendarState()

	fun requestActivity() {
		mainModel.launch {
			val result = ClientAPI.request(
				route = API.User.Activity.GetActivities
			)
			if (result is Data.Success) {
				val data = result.data
				activities.replaceAll(data)

				calendarState.events.clear()
				for (activity in activities) {
					val ts = activity.ts
					val title = activity.title
					if (ts != null && title != null) {
						try {
							val date = DateEx.Formatter.standardDate.parse(ts)
							calendarState.events.put(date, title)
						}
						catch (_: CancellationException) { ensureActive() }
						catch (_: Throwable) { }
					}
				}
			}
		}
	}

	fun onRefreshCalendar() {

	}

	fun onCalendarEvent(date: LocalDate) {

	}
}

@Composable
private fun Portrait(
	model: WorldModel
) {
	Column(
		modifier = Modifier.fillMaxSize(),
		verticalArrangement = Arrangement.SpaceBetween
	) {
		val pics by rememberDerivedState {
			val banners = mutableListOf<Picture>()
			for (activity in model.activities) {
				activity.picPath?.let { banners += Picture(it) }
			}
			banners
		}

		Banner(
			pics = pics,
			spacing = 40.dp,
			interval = 3000L,
			modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
		) { pic, scale ->
			WebImage(
				uri = pic.image,
				contentScale = ContentScale.Crop,
				modifier = Modifier.fillMaxWidth().aspectRatio(2f)
					.scale(scale).clip(MaterialTheme.shapes.medium)
			)
		}
		Surface(
			modifier = Modifier.fillMaxWidth().padding(10.dp),
			shape = MaterialTheme.shapes.extraLarge,
			shadowElevation = 5.dp
		) {
			Calendar(
				state = model.calendarState,
				modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
				actions = {
					if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
						ClickIcon(
							imageVector = Icons.Default.Add,
							onClick = {}
						)
					}
					ClickIcon(
						imageVector = Icons.Default.Refresh,
						onClick = { model.onRefreshCalendar() }
					)
				},
				onEventClick = { model.onCalendarEvent(it) }
			)
		}
	}
}

@Composable
private fun Landscape(
	model: WorldModel
) {

}


@Composable
fun ScreenWorld(model: MainModel) {
	if (app.isPortrait) Portrait(model = model.worldModel)
	else Landscape(model = model.worldModel)

	LaunchOnce(model.worldModel.flagFirstLoad) {
		model.worldModel.requestActivity()
	}
}