package love.yinlin.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import love.yinlin.compose.screen.DataSource
import love.yinlin.data.rachel.activity.Activity

@Stable
object DataSourceInformation : DataSource {
    // 活动
    val activities = mutableStateListOf<Activity>()

    override fun onDataSourceClean() {
        activities.clear()
    }
}