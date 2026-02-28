package love.yinlin.data.rachel.discovery

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.data.rachel.topic.Comment

@Stable
internal enum class DiscoveryItem(val id: Int, val icon: ImageVector) {
    LatestTopic(Comment.Section.LATEST_TOPIC, Icons.NewReleases),
    LatestComment(Comment.Section.LATEST_COMMENT, Icons.NewReleases),
    Hot(Comment.Section.HOT, Icons.LocalFireDepartment),
    Notification(Comment.Section.NOTIFICATION, Icons.Campaign),
    Water(Comment.Section.WATER, Icons.WaterDrop),
    Activity(Comment.Section.ACTIVITY, Icons.Celebration),
    Discussion(Comment.Section.DISCUSSION, Icons.Chat);
}