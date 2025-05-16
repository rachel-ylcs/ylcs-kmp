package love.yinlin.ui.screen.community

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.data.rachel.follows.FollowInfo
import love.yinlin.data.rachel.follows.FollowerInfo
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.ui.component.layout.Pagination
import love.yinlin.ui.component.layout.PaginationArgs
import love.yinlin.ui.component.screen.SubScreen

@Stable
enum class FollowTabItem(val title: String) {
    FOLLOWS("关注"), FOLLOWERS("粉丝"), BLOCK_USERS("黑名单");

    companion object {
        fun fromInt(value: Int): FollowTabItem = when (value) {
            FOLLOWERS.ordinal -> FOLLOWERS
            BLOCK_USERS.ordinal -> BLOCK_USERS
            else -> FOLLOWS
        }
    }
}

@Stable
class ScreenFollows(model: AppModel, args: Args) : SubScreen<ScreenFollows.Args>(model) {
    @Stable
    @Serializable
    data class Args(val tab: Int)

    private val tab by mutableStateOf(FollowTabItem.fromInt(args.tab))

    val pageFollows = object : PaginationArgs<FollowInfo, Int, Long>(Int.MAX_VALUE, 0L) {
        override fun offset(item: FollowInfo): Int = item.score
        override fun arg1(item: FollowInfo): Long = item.fid
    }

    val pageFollowers = object : PaginationArgs<FollowerInfo, Int, Long>(Int.MAX_VALUE, 0L) {
        override fun offset(item: FollowerInfo): Int = item.score
        override fun arg1(item: FollowerInfo): Long = item.fid
    }

    override val title: String? by derivedStateOf { tab.title }

    @Composable
    override fun SubContent(device: Device) {

    }
}