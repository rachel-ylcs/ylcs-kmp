package love.yinlin.ui.screen.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.api.Default
import love.yinlin.data.Data
import love.yinlin.data.rachel.Comment
import love.yinlin.data.rachel.Topic
import love.yinlin.extension.LaunchFlag
import love.yinlin.extension.LaunchOnce
import love.yinlin.extension.replaceAll
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.layout.TabBar
import love.yinlin.ui.screen.MainModel

@Stable
private enum class DiscoveryItem(
	val id: Int,
	val title: String,
	val icon: ImageVector
) {
	Lastest(Comment.Section.LATEST, "最新", Icons.Filled.NewReleases),
	Hot(Comment.Section.HOT, "热门", Icons.Filled.LocalFireDepartment),
	Notification(Comment.Section.NOTIFICATION, "公告", Icons.Filled.Campaign),
	Water(Comment.Section.WATER, "水贴", Icons.Filled.WaterDrop),
	Activity(Comment.Section.ACTIVITY, "活动", Icons.Filled.Celebration),
	Discussion(Comment.Section.DISCUSSION, "交流", Icons.AutoMirrored.Filled.Chat),
}

class DiscoveryModel(val mainModel: MainModel) {
	val launchFlag = LaunchFlag()
	var state by mutableStateOf(BoxState.EMPTY)

	var currentPage by mutableIntStateOf(0)
	val items = mutableStateListOf<Topic>()
	var offset: Int = Int.MAX_VALUE
	var canLoading by mutableStateOf(false)

	fun requestNewData() {
		mainModel.launch {
			val section = DiscoveryItem.entries[currentPage].id
			state = BoxState.LOADING
			val result = when (currentPage) {
				DiscoveryItem.Lastest.ordinal -> ClientAPI.request(
					route = API.User.Topic.GetLatestTopics,
					data = Default.PageDescRequest()
				)
				DiscoveryItem.Hot.ordinal -> ClientAPI.request(
					route = API.User.Topic.GetHotTopics,
					data = Default.PageDescRequest()
				)
				else -> ClientAPI.request(
					route = API.User.Topic.GetSectionTopics,
					data = API.User.Topic.GetSectionTopics.Request(section = section)
				)
			}
			if (result is Data.Success) {
				val topics = result.data
				items.replaceAll(topics)

				val last = topics.lastOrNull()
				offset = when (section) {
					DiscoveryItem.Lastest.id -> last?.tid
					DiscoveryItem.Hot.id -> last?.coinNum
					else -> last?.tid
				} ?: Int.MAX_VALUE

				if (topics.isEmpty()) {
					state = BoxState.EMPTY
					canLoading = false
				}
				else {
					state = BoxState.CONTENT
					canLoading = true
				}
			}
			else state = BoxState.NETWORK_ERROR
		}
	}

	fun requestMoreData() {

	}

	fun onNavigate(index: Int) {
		currentPage = index
		requestNewData()
	}
}

@Composable
fun ScreenDiscovery(model: DiscoveryModel) {
	val tabItems = remember { DiscoveryItem.entries.map { it.title to it.icon } }

	StatefulBox(
		state = model.state,
		modifier = Modifier.fillMaxSize()
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			Surface(
				modifier = Modifier.fillMaxWidth().zIndex(5f),
				shadowElevation = 5.dp
			) {
				Row(
					modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(10.dp)
				) {
					TabBar(
						currentPage = model.currentPage,
						onNavigate = { model.onNavigate(it) },
						items = tabItems,
						modifier = Modifier.weight(1f)
					)
					ClickIcon(
						imageVector = Icons.Filled.Refresh,
						onClick = { model.requestNewData() }
					)
				}
			}
			PaginationStaggeredGrid(
				items = model.items,
				key = { it.tid },
				columns = StaggeredGridCells.Adaptive(150.dp),
				canRefresh = true,
				canLoading = model.canLoading,
				onRefresh = { model.requestNewData() },
				onLoading = { model.requestMoreData() },
				modifier = Modifier.fillMaxWidth().weight(1f),
				contentPadding = PaddingValues(10.dp),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
				verticalItemSpacing = 10.dp
			) { topic ->

			}
		}
	}

	LaunchOnce(model.launchFlag) {
		model.requestNewData()
	}
}