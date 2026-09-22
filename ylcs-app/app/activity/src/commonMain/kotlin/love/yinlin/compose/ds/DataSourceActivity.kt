package love.yinlin.compose.ds

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import love.yinlin.app
import love.yinlin.cs.*
import love.yinlin.data.Data
import love.yinlin.data.map
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.DateEx
import love.yinlin.extension.findAssign
import love.yinlin.extension.findModify
import love.yinlin.extension.replaceAll
import love.yinlin.fs.File

@Stable
object DataSourceActivity {
    // 活动
    val activities: List<Activity> field = mutableStateListOf<Activity>()

    // 当前展示活动
    val spanActivities: List<Pair<LocalDate, Activity?>> by derivedStateOf {
        // 取前5天 + 今天 + 后10天的记录
        val today = DateEx.Today
        val activityMap = activities.asSequence().mapNotNull { activity ->
            val date = activity.ts?.let(DateEx.Formatter.standardDate::parse)
            if (date != null) date to activity else null
        }.toMap()
        (-5 .. 10).map { index ->
            val date = today.plus(index, DateTimeUnit.DAY)
            date to activityMap[date]
        }
    }

    suspend fun requestNewActivity() {
        ApiActivityGetActivities.request(app.config.userToken.ifEmpty { null }) {
            activities.replaceAll(it.sorted())
        }
    }

    suspend fun deleteActivity(aid: Int): Throwable? {
        return ApiActivityDeleteActivity.request(app.config.userToken, aid) {
            activities.findModify(predicate = { it.aid == aid }) { this -= it }
        }
    }

    suspend fun updateActivity(aid: Int, newActivity: Activity, block: (Activity) -> Activity): Throwable? {
        return ApiActivityUpdateActivityInfo.request(app.config.userToken, newActivity) {
            activities.findAssign(predicate = { it.aid == aid }, block)
        }
    }

    suspend fun updateActivityPhoto(aid: Int, key: String, path: File, block: (String, Activity) -> Activity): Throwable? {
        return ApiActivityUpdateActivityPhoto.request(app.config.userToken, aid, key, apiFile(path)) { newPic ->
            activities.findAssign(predicate = { it.aid == aid }) {
                block(newPic, it)
            }
        }
    }

    suspend fun deleteActivityPhoto(aid: Int, key: String, block: (Activity) -> Activity): Throwable? {
        return ApiActivityDeleteActivityPhoto.request(app.config.userToken, aid, key) {
            activities.findAssign(predicate = { it.aid == aid }, block)
        }
    }

    suspend fun addActivityPhotos(aid: Int, key: String, files: List<File>, block: (List<String>, Activity) -> Activity): Throwable? {
        return ApiActivityAddActivityPhotos.request(app.config.userToken, aid, key, apiFile(files)!!) { newPics ->
            activities.findAssign(predicate = { it.aid == aid}) {
                block(newPics, it)
            }
        }
    }

    suspend fun updateActivityPhotos(aid: Int, key: String, index: Int, path: File, block: (String, Activity) -> Activity): Throwable? {
        return ApiActivityUpdateActivityPhotos.request(app.config.userToken, aid, key, index, apiFile(path)) { newPic ->
            activities.findAssign(predicate = { it.aid == aid }) {
                block(newPic, it)
            }
        }
    }

    suspend fun deleteActivityPhotos(aid: Int, key: String, index: Int, block: (Activity) -> Activity): Throwable? {
        return ApiActivityDeleteActivityPhotos.request(app.config.userToken, aid, key, index) {
            activities.findAssign(predicate = { it.aid == aid }, block)
        }
    }

    suspend fun addActivity(hide: Boolean): Data<Int> {
        val result = ApiActivityAddActivity.request(app.config.userToken, hide).map { it.o1 }
        if (result is Data.Success) requestNewActivity()
        return result
    }
}