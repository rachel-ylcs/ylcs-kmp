package love.yinlin.data.rachel.literal

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import love.yinlin.compose.Colors
import love.yinlin.compose.ui.icon.Icons

@Stable
internal object AppAbout {
     val updateInfo = AppUpdateInfo(
        platform = "Android/iOS/Windows/Linux/macOS/Web 发布",
        title = "银临茶舍 统一资讯源 演出记忆",
        force = true,
        maintenance = true,
        date = LocalDate(2026, 10, 18),
        groups = [
            AppUpdateRecordGroup(
                type = "特性",
                icon = Icons.RocketLaunch,
                color = Colors.Steel5,
                background = Colors.Steel2,
                records = [
                    "1. 统一资讯源支持（微博、超话、B站、小红书、抖音）",
                    "2. 银临的剪刀手电台支持（本地缓存电台节目离线播放）",
                    "3. 演出记忆板块支持（聆听每一场演出的回忆）",
                    "4. iOS端，Linux端和macos端支持音频视频播放",
                    "5. 安卓端支持Android17"
                ]
            ),
            AppUpdateRecordGroup(
                type = "优化",
                icon = Icons.Speed,
                color = Colors.Pink4,
                background = Colors.Pink2,
                records = [
                    "1. 重构所有客户端的UI界面",
                    "2. 去除几乎所有曲库歌单更新时要求停止播放器的限制",
                    "3. 九宫格图片超出九张时显示更多数量",
                    "4. 桌面端支持批量下载资讯源图片",
                    "5. 减少安卓端安装包大小，稍增桌面端大小，提升运行性能",
                ]
            ),
            AppUpdateRecordGroup(
                type = "修复",
                icon = Icons.BugReport,
                color = Colors.Red4,
                background = Colors.Red2,
                records = [
                    "1. 修复平台下载MOD歌手名称错误的问题",
                    "2. 修复裁剪框点击外部消失的问题",
                    "3. 修复MOD导入后当前播放列表数据异常崩溃的问题",
                    "4. 修复歌单移动歌曲索引异常的问题"
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