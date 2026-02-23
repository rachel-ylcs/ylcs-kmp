package love.yinlin.data.rachel.literal

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import love.yinlin.compose.Colors
import love.yinlin.compose.ui.icon.Icons

@Stable
internal object AppAbout {
     val updateInfo = AppUpdateInfo(
        platform = "Android/Windows/Web 先行发布",
        title = "音游内测上线, 动态歌词引擎支持, 抽奖活动即将上线",
        force = true,
        maintenance = false,
        date = LocalDate(2025, 12, 15),
        groups = listOf(
            AppUpdateRecordGroup(
                type = "新功能",
                icon = Icons.RocketLaunch,
                color = Colors.Steel5,
                background = Colors.Steel2,
                records = listOf(
                    "1. 音游内测上线, 内测期将陆续支持银临全曲集, 公测后将开放玩家排行榜",
                    "2. 动态歌词引擎支持, 现在歌词播放与悬浮歌词将支持逐字歌词",
                    "3. 抽奖活动上线, 节日活动将会在APP中发放福利抽奖",
                )
            ),
            AppUpdateRecordGroup(
                type = "优化",
                icon = Icons.Speed,
                color = Colors.Pink4,
                background = Colors.Pink2,
                records = listOf(
                    "1. Windows端减少20M包体积, 移除vlc依赖(可手动删除)",
                    "2. Android端减少10M包体积",
                    "3. 活动页新增隐藏活动预告",
                    "4. 美图图集页标题也加入搜索范围",
                    "5. 优化美图图集图片加载, 选择原图后需要下载才能生效",
                    "6. 优化歌曲详情页配色与布局",
                    "7. 增大编辑器表情的显示大小",
                    "8. 部分MOD资源增加删除功能",
                )
            ),
            AppUpdateRecordGroup(
                type = "修复",
                icon = Icons.BugReport,
                color = Colors.Red4,
                background = Colors.Red2,
                records = listOf(
                    "1. 修复只有一首歌曲循环播放结束时悬浮歌词不消失的问题",
                    "2. 修复修改密码显示不一致的问题",
                    "3. 修复服务器总是返回非法异常的问题",
                    "4. 修复歌单云备份错误未提示的问题",
                    "5. 修复社区和微博页滚动时卡死的问题",
                    "6. 修复拖拽悬浮歌词配置异常的问题",
                    "7. 修复Windows音乐播放器进度间隔固定的问题",
                    "8. 修复退出视频页后声音仍在播放的问题",
                    "9. 修复音频播放器结束时未停止的问题",
                    "10. 修复初始服务未加载完成时操作崩溃的问题",
                    "11. 修复账号有效期异常退出登录的问题",
                    "12. 修复网页端微博和超话显示异常的问题",
                    "13. 修复九宫格图片点击闪退的问题",
                    "14. 修复电脑端无法进入音游的问题",
                )
            )
        )
    )

    val contributors = listOf(
        AppContributorGroup("设计", Icons.Brush, Colors.Pink4, listOf(
            AppContributor("方旖旎", 7),
            AppContributor("木棠", 1563),
            AppContributor("竹香满亭", 11),
            AppContributor("尘落", 89)
        )),
        AppContributorGroup("运营", Icons.Store, Colors.Steel4, listOf(
            AppContributor("思懿", 6),
            AppContributor("清逸", 74),
            AppContributor("青栀", 87)
        )),
        AppContributorGroup("宣发", Icons.Campaign, Colors.Red4, listOf(
            AppContributor("姜辞", 5),
            AppContributor("晨晨", 15)
        )),
        AppContributorGroup("经办", Icons.LocalFireDepartment, Colors.Orange4, listOf(
            AppContributor("寒山", 1),
            AppContributor("南溟", 3),
            AppContributor("韩非", 12),
            AppContributor("泸沽寻临", 8),
            AppContributor("名字不太喵", 18),
            AppContributor("圈圈临", 2)
        )),
        AppContributorGroup("数据", Icons.Token, Colors.Purple4, listOf(
            AppContributor("海屿悼词", 9),
            AppContributor("桑檀", 1768),
        )),
        AppContributorGroup("开发", Icons.Code, Colors.Green4, listOf(
            AppContributor("焦骨", 10),
            AppContributor("青尘", 1524),
            AppContributor("yingfeng", 14),
            AppContributor("双花", 429),
            AppContributor("桃花坞里桃花庵", 20),
            AppContributor("苏晚卿", 359)
        )),
    )
}