package love.yinlin.data.rachel.literal

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import love.yinlin.compose.Colors
import love.yinlin.compose.ui.icon.Icons

@Stable
internal object AppAbout {
     val updateInfo = AppUpdateInfo(
        platform = "全平台发布",
        title = "银临茶舍 原生桌面端上线",
        force = true,
        maintenance = true,
        date = LocalDate(2026, 11, 1),
        groups = [
            AppUpdateRecordGroup(
                type = "特性",
                icon = Icons.RocketLaunch,
                color = Colors.Steel5,
                background = Colors.Steel2,
                records = [
                    "1. 统一资讯源支持（微博、超话、B站、小红书、抖音）",
                    "2. 银临的剪刀手电台支持（本地缓存电台节目离线播放）",
                    "3. 演出记忆板块支持（聆听每一场演出的回忆）"
                ]
            ),
            AppUpdateRecordGroup(
                type = "优化",
                icon = Icons.Speed,
                color = Colors.Pink4,
                background = Colors.Pink2,
                records = [
                    "1. Android端向下兼容安卓9，优化安装包体积",
                    "2. 桌面原生客户端上线，极致性能，支持windows, linux, macos",
                    "3. 全平台支持音视频播放",
                    "4. 移除在线更新及更新包的功能",
                    "5. 为部分页面提供返回键",
                ]
            ),
            AppUpdateRecordGroup(
                type = "修复",
                icon = Icons.BugReport,
                color = Colors.Red4,
                background = Colors.Red2,
                records = [
                    "1. 修复切换资讯源卡住的问题",
                    "3. 修复iOS登录崩溃的问题",
                    "3. 修复电台断网下无法浏览的问题",
                    "4. 修复未登录时无法进入登录页的问题"
                ]
            )
        ]
    )

    val contributors = [
        AppContributorGroup("设计", Icons.Brush, Colors.Pink4, [
            AppContributor("方旖旎", 7),
            AppContributor("木棠", 1563),
            AppContributor("竹香满亭", 11),
            AppContributor("尘落", 89),
        ]),
        AppContributorGroup("运营", Icons.Store, Colors.Steel4, [
            AppContributor("思懿", 6),
            AppContributor("清逸", 74),
            AppContributor("青栀", 87),
        ]),
        AppContributorGroup("宣发", Icons.Campaign, Colors.Red4, [
            AppContributor("姜辞", 5),
            AppContributor("晨晨", 15),
        ]),
        AppContributorGroup("经办", Icons.LocalFireDepartment, Colors.Orange4, [
            AppContributor("寒山", 1),
            AppContributor("南溟", 3),
            AppContributor("韩非", 12),
            AppContributor("泸沽寻临", 8),
            AppContributor("名字不太喵", 18),
            AppContributor("圈圈临", 2),
        ]),
        AppContributorGroup("数据", Icons.Token, Colors.Purple4, [
            AppContributor("海屿悼词", 9),
            AppContributor("桑檀", 1768),
            AppContributor("黎耘", 1844),
            AppContributor("熔金", 856)
        ]),
        AppContributorGroup("开发", Icons.Code, Colors.Green4, [
            AppContributor("焦骨", 10),
            AppContributor("青尘", 1524),
            AppContributor("yingfeng", 14),
            AppContributor("双花", 429),
            AppContributor("桃花坞里桃花庵", 20),
            AppContributor("苏晚卿", 359),
        ]),
    ]
}