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
import love.yinlin.common.Colors
import love.yinlin.common.ThemeValue
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
        platform = "Android/Windows/Linux/Web 先行发布\niOS/MacOS 晚上发布",
        title = "测试标题",
        force = false,
        maintenance = false,
        date = LocalDate(2025, 6, 22),
        groups = listOf(
            UpdateRecordGroup(
                type = "新功能",
                icon = Icons.Outlined.RocketLaunch,
                color = Colors.Steel4,
                background = Colors.Steel2,
                records = listOf(
                    "1. 资讯接入银临抖音板块与视频下载",
                    """
2. 经验系统上线
  [经验] 本次更新后与当前银币数同步
  [等级] 仅与经验相关, 银币独立
  [银币获取] 途径保持不变
  [经验获取] 签到+1, 投币+N, 主题被三连+1, 游戏结算+N/2, 参与游戏+1, 福利/奖励等
                    """.trimIndent(),
                    "3. 所有点击图标均添加悬浮气泡文字提示, 可在设置中关闭",
                    "4. 小游戏新增[你画我猜]",
                    "5. 桌面端支持窗口置顶, 系统托盘图标",
                )
            ),
            UpdateRecordGroup(
                type = "优化",
                icon = Icons.Outlined.Speed,
                color = Colors.Pink4,
                background = Colors.Pink2,
                records = listOf(
                    "1. 所有图片加载将显示进度圈和状态遮罩",
                    "2. 更新字体以支持更多字符",
                    "3. 重构滑块条UI",
                    "4. 点击歌曲封面跳转到详情页",
                    """
5. 小游戏优化
  [答题] 选择题或填空题完成后自动跳转下一题, 正确率最小限制调整为0.6
  [寻花令] 输入框自动获取焦点
  [歌词对战] 断线后可重新回到大厅, 如有未做的题提交前提示, 显示答案的字数要求
                    """.trimIndent(),
                )
            ),
            UpdateRecordGroup(
                type = "修复",
                icon = Icons.Outlined.BugReport,
                color = Colors.Red4,
                background = Colors.Red2,
                records = listOf(
                    "1. 修复网页端无法进入在线歌词对战",
                    "2. 修复美图页过长标题被遮挡",
                    "3. 修复微博超话页下载视频或图片不显示进度框",
                    "4. 修复单曲循环时播放完毕歌词不滚动到初始状态"
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
            Contributor("鲤鱼焙梨", 13),
            Contributor("海屿悼词", 9),
            Contributor("冰临", 17),
            Contributor("银小临", 16)
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
                        .padding(ThemeValue.Padding.Value),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.LittleSpace, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniIcon(
                        icon = contributorGroup.icon,
                        size = ThemeValue.Size.MicroIcon
                    )
                    Text(
                        text = contributorGroup.title,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                Column(modifier = Modifier.padding(vertical = ThemeValue.Border.Small / 2).weight(2f).border(
                    width = ThemeValue.Border.Small,
                    color = contributorGroup.color.copy(0.7f)
                )) {
                    for (contributor in contributorGroup.names) {
                        Text(
                            text = contributor.name,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onClick(contributor) }
                                .padding(ThemeValue.Padding.ExtraValue),
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
        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace),
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
            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
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
                        modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue),
                        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace),
                    ) {
                        RachelText(
                            text = group.type,
                            icon = group.icon,
                            color = group.color,
                            style = MaterialTheme.typography.labelMedium,
                            padding = ThemeValue.Padding.ZeroValue
                        )
                        Space()
                        for (record in group.records) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier
                                    .size(ThemeValue.Size.dotHeight)
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