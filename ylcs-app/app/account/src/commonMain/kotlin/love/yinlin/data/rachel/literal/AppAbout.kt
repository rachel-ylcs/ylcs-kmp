package love.yinlin.data.rachel.literal

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import love.yinlin.compose.Colors
import love.yinlin.compose.ui.icon.Icons

@Stable
internal object AppAbout {
     val updateInfo = AppUpdateInfo(
        platform = "Android/Windows/Linux/macOS/Web 先行发布",
        title = "银临茶舍 伴奏功能上线",
        force = false,
        maintenance = false,
        date = LocalDate(2026, 3, 10),
        groups = listOf(
            AppUpdateRecordGroup(
                type = "新功能",
                icon = Icons.RocketLaunch,
                color = Colors.Steel5,
                background = Colors.Steel2,
                records = listOf(
                    "1. 伴奏系统上线, 全曲库纯净伴奏解锁",
                    "2. 动态歌词引擎支持, 与音游共享配置",
                    "3. 官网落地, 支持在线下载和查看使用教程",
                    "4. 音游内测上线, 内测期将陆续支持银临全曲集, 公测后将开放玩家排行榜",
                )
            ),
            AppUpdateRecordGroup(
                type = "优化",
                icon = Icons.Speed,
                color = Colors.Pink4,
                background = Colors.Pink2,
                records = listOf(
                    "1. 曲库和工坊新增MOD筛选查找",
                    "2. 歌单与播放列表显示歌曲数量",
                    "3. 歌单歌曲单击后立即从此位置播放该歌单",
                    "4. 工坊迁移到音乐首页",
                    "5. 优化安卓端性能",
                )
            ),
            AppUpdateRecordGroup(
                type = "修复",
                icon = Icons.BugReport,
                color = Colors.Red4,
                background = Colors.Red2,
                records = listOf(
                    "1. 修复安卓端悬浮歌词崩溃的问题",
                    "2. 修复电脑端表情显示崩溃的问题",
                    "3. 修复对话框边距的问题",
                    "4. 修复歌单顺序调整异常的问题",
                    "5. 修复歌词引擎切换时歌词空置的问题",
                    "6. 修复页面切换未防抖的问题",
                )
            )
        )
    )

    val contributors = listOf(
        AppContributorGroup("设计", Icons.Brush, Colors.Pink4, listOf(
            AppContributor("方旖旎", 7),
            AppContributor("木棠", 1563),
            AppContributor("竹香满亭", 11),
            AppContributor("尘落", 89),
        )),
        AppContributorGroup("运营", Icons.Store, Colors.Steel4, listOf(
            AppContributor("思懿", 6),
            AppContributor("清逸", 74),
            AppContributor("青栀", 87),
        )),
        AppContributorGroup("宣发", Icons.Campaign, Colors.Red4, listOf(
            AppContributor("姜辞", 5),
            AppContributor("晨晨", 15),
        )),
        AppContributorGroup("经办", Icons.LocalFireDepartment, Colors.Orange4, listOf(
            AppContributor("寒山", 1),
            AppContributor("南溟", 3),
            AppContributor("韩非", 12),
            AppContributor("泸沽寻临", 8),
            AppContributor("名字不太喵", 18),
            AppContributor("圈圈临", 2),
        )),
        AppContributorGroup("数据", Icons.Token, Colors.Purple4, listOf(
            AppContributor("海屿悼词", 9),
            AppContributor("桑檀", 1768),
            AppContributor("黎耘", 1844),
        )),
        AppContributorGroup("开发", Icons.Code, Colors.Green4, listOf(
            AppContributor("焦骨", 10),
            AppContributor("青尘", 1524),
            AppContributor("yingfeng", 14),
            AppContributor("双花", 429),
            AppContributor("桃花坞里桃花庵", 20),
            AppContributor("苏晚卿", 359),
        )),
    )
}