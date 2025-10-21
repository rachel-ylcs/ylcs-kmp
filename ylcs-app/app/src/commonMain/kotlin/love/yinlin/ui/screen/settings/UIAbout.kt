package love.yinlin.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.datetime.LocalDate
import love.yinlin.Local
import love.yinlin.compose.*
import love.yinlin.extension.DateEx
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.screen.community.BoxText

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

object About {
    internal val updateInfo = UpdateInfo(
        platform = "Android/Windows修复更新",
        title = "本更新仅修复图集与微博部分, 大版本更新将与2025新巡演同步",
        force = false,
        maintenance = false,
        date = LocalDate(2025, 10, 1),
        groups = listOf(
            UpdateRecordGroup(
                type = "新功能",
                icon = Icons.Outlined.RocketLaunch,
                color = Colors.Steel4,
                background = Colors.Steel2,
                records = listOf(
                    "预热1: 音游即将上线, 银临全曲集陆续支持...",
                    "预热2: 在线曲库上线, 所有歌曲MOD采用在线下载, 按需更新资源包...",
                    "预热3: 抽奖活动上线, 节日活动或福利抽奖均在APP中进行..."
                )
            ),
            UpdateRecordGroup(
                type = "优化",
                icon = Icons.Outlined.Speed,
                color = Colors.Pink4,
                background = Colors.Pink2,
                records = listOf()
            ),
            UpdateRecordGroup(
                type = "修复",
                icon = Icons.Outlined.BugReport,
                color = Colors.Red4,
                background = Colors.Red2,
                records = listOf(
                    "1. 修复图集下载异常的问题",
                    "2. 修复微博和超话加载异常的问题"
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

@Composable
internal fun ContributorList(
    contributors: Array<ContributorGroup>,
    modifier: Modifier = Modifier,
    onClick: (Contributor) -> Unit
) {
    Column(modifier = modifier) {
        for (contributorGroup in contributors) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.weight(1f)
                        .background(contributorGroup.color.copy(alpha = 0.7f))
                        .padding(CustomTheme.padding.value),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.littleSpace, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniIcon(
                        icon = contributorGroup.icon,
                        size = CustomTheme.size.microIcon
                    )
                    Text(
                        text = contributorGroup.title,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                Column(modifier = Modifier.padding(vertical = CustomTheme.border.small / 2).weight(2f).border(
                    width = CustomTheme.border.small,
                    color = contributorGroup.color.copy(0.7f)
                )) {
                    for (contributor in contributorGroup.names) {
                        Text(
                            text = contributor.name,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onClick(contributor) }
                                .padding(CustomTheme.padding.extraValue),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun UpdateInfoLayout(
    updateInfo: UpdateInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace),
    ) {
        Text(
            text = "${Local.NAME} ${Local.VERSION_NAME} 更新日志",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Space()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (updateInfo.force) BoxText(text = "强制更新", color = MaterialTheme.colorScheme.primary)
            if (updateInfo.maintenance) BoxText(text = "停服维护", color = MaterialTheme.colorScheme.secondary)
            Text(
                text = remember { DateEx.Formatter.standardDate.format(updateInfo.date) ?: "" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = updateInfo.platform,
            modifier = Modifier.fillMaxWidth()
        )
        updateInfo.title?.let { title ->
            Text(
                text = title,
                color = CustomTheme.colorScheme.warning,
                modifier = Modifier.fillMaxWidth()
            )
        }
        for (group in updateInfo.groups) {
            if (group.records.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = group.background.copy(alpha = 0.2f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue),
                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace),
                    ) {
                        RachelText(
                            text = group.type,
                            icon = group.icon,
                            color = group.color,
                            style = MaterialTheme.typography.labelMedium,
                            padding = CustomTheme.padding.zeroValue
                        )
                        Space()
                        for (record in group.records) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier
                                    .size(CustomTheme.size.dotHeight)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface)
                                )
                                Text(text = record)
                            }
                        }
                    }
                }
            }
        }
    }
}