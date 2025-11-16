package love.yinlin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate
import love.yinlin.compose.Colors

@Stable
data object About {
    internal val updateInfo = UpdateInfo(
        platform = "Android/Windows修复更新",
        title = "新版本更新加速进行中... 与粼粼一起前进吧!",
        force = true,
        maintenance = true,
        date = LocalDate(2025, 11, 16),
        groups = listOf(
            UpdateRecordGroup(
                type = "新功能",
                icon = Icons.Outlined.RocketLaunch,
                color = Colors.Steel4,
                background = Colors.Steel2,
                records = listOf(
                    "预热1: 音游即将上线, 银临全曲集陆续支持...",
                    "预热2: 抽奖活动上线, 节日活动或福利抽奖均在APP中进行..."
                )
            ),
            UpdateRecordGroup(
                type = "优化",
                icon = Icons.Outlined.Speed,
                color = Colors.Pink4,
                background = Colors.Pink2,
                records = listOf(
                    "1. 在线曲库上线, 所有歌曲MOD均可在线下载, 按需更新资源包",
                    "2. MOD歌曲详情页、在线详情页、歌评页合并, 一览无余",
                    "3. 演出活动页UI重绘, 更加丰富的演出信息展示",
                    "4. 曲库按钮平铺, 更容易操作",
                    "5. APP优化, 大幅度减小包体积"
                )
            ),
            UpdateRecordGroup(
                type = "修复",
                icon = Icons.Outlined.BugReport,
                color = Colors.Red4,
                background = Colors.Red2,
                records = listOf(
                    "1. 修复具有多行输入的文本框取消其IME回车响应的问题",
                    "2. 修复微博和超话加载异常的问题",
                )
            )
        )
    )

    internal val contributors = arrayOf(
        ContributorGroup("设计", Icons.Outlined.Brush, Colors.Pink4, listOf(
            Contributor("方旖旎", 7),
            Contributor("木棠", 1563),
            Contributor("竹香满亭", 11),
            Contributor("尘落", 89)
        )),
        ContributorGroup("运营", Icons.Outlined.Store, Colors.Steel4, listOf(
            Contributor("思懿", 6),
            Contributor("清逸", 74),
            Contributor("青栀", 87)
        )),
        ContributorGroup("宣发", Icons.Outlined.Campaign, Colors.Red4, listOf(
            Contributor("姜辞", 5),
            Contributor("晨晨", 15)
        )),
        ContributorGroup("经办", Icons.Outlined.LocalFireDepartment, Colors.Orange4, listOf(
            Contributor("寒山", 1),
            Contributor("南溟", 3),
            Contributor("韩非", 12),
            Contributor("泸沽寻临", 8),
            Contributor("名字不太喵", 18),
            Contributor("圈圈临", 2)
        )),
        ContributorGroup("数据", Icons.Outlined.Token, Colors.Purple4, listOf(
            Contributor("海屿悼词", 9),
            Contributor("桑檀", 1768),
        )),
        ContributorGroup("开发", Icons.Outlined.Code, Colors.Green4, listOf(
            Contributor("焦骨", 10),
            Contributor("青尘", 1524),
            Contributor("yingfeng", 14),
            Contributor("桃花坞里桃花庵", 20),
            Contributor("双花", 429),
            Contributor("苏晚卿", 359)
        )),
    )
}

@Stable
internal data class Contributor(
    val name: String,
    val uid: Int
)

@Stable
internal data class ContributorGroup(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val names: List<Contributor>
)

@Stable
internal data class UpdateRecordGroup(
    val type: String,
    val icon: ImageVector,
    val color: Color,
    val background: Color,
    val records: List<String>
)

@Stable
internal data class UpdateInfo(
    val platform: String,
    val title: String?,
    val force: Boolean,
    val maintenance: Boolean,
    val date: LocalDate,
    val groups: List<UpdateRecordGroup>
)